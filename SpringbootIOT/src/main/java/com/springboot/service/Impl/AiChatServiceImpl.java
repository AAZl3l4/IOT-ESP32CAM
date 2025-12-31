package com.springboot.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.service.AiChatService;
import com.springboot.service.CamService;
import com.springboot.service.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI对话服务实现
 * 使用HTTP调用ModelScope Qwen-VL视觉语言模型
 * 异步执行，完成后通过SSE推送结果
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {
    
    @Value("${modelscope.api-key}")
    private String apiKey;
    
    @Value("${modelscope.model}")
    private String model;
    
    @Autowired
    private CamService camService;
    
    @Autowired
    private SseService sseService;
    
    @Value("${modelscope.api-url}")
    private String apiUrl;
    
    @Value("${photos-dir}")
    private String photosDir;
    
    @Value("${ai.max-wait-seconds}")
    private int maxWaitSeconds;
    
    /** 会话历史缓存 */
    private final ConcurrentHashMap<String, List<Map<String, Object>>> sessionHistory = new ConcurrentHashMap<>();
    
    /** 简化的历史记录 */
    private final ConcurrentHashMap<String, List<Map<String, String>>> simpleHistory = new ConcurrentHashMap<>();
    
    /** 任务ID生成器 */
    private final AtomicLong taskIdGenerator = new AtomicLong(System.currentTimeMillis());
    
    /** 异步执行器 */
    private ExecutorService executor;
    
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    
    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
        objectMapper = new ObjectMapper();
        executor = Executors.newFixedThreadPool(3);
        log.info("ModelScope AI服务初始化完成, model={}", model);
    }
    
    @Override
    public String chatWithCaptureAsync(String clientId, String sessionId, String message) {
        String taskId = String.valueOf(taskIdGenerator.incrementAndGet());
        
        // 触发拍照
        log.info("触发ESP32拍照: clientId={}, taskId={}", clientId, taskId);
        camService.triggerCapture(clientId);
        
        // 异步执行：等待图片+调用AI
        executor.submit(() -> {
            try {
                // 等待图片上传
                File imageFile = waitForImageFile(clientId);
                if (imageFile == null) {
                    sseService.pushAiResponse(sessionId, taskId, "❌ 等待拍照超时，请确认ESP32在线", "");
                    return;
                }
                log.info("获取到图片: {}, taskId={}", imageFile.getName(), taskId);
                
                // 读取图片并调用AI
                byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
                String aiResponse = callAiWithImage(sessionId, message, imageBytes);
                
                // 推送结果
                sseService.pushAiResponse(sessionId, taskId, aiResponse, imageFile.getName());
                
            } catch (Exception e) {
                log.error("AI调用失败: taskId={}, error={}", taskId, e.getMessage(), e);
                sseService.pushAiResponse(sessionId, taskId, "❌ AI服务调用失败: " + e.getMessage(), "");
            }
        });
        
        return taskId;
    }
    
    @Override
    public String analyzeImageAsync(String sessionId, String imageFile, String message) {
        String taskId = String.valueOf(taskIdGenerator.incrementAndGet());
        
        // 异步执行AI分析
        executor.submit(() -> {
            try {
                Path imagePath = Paths.get(photosDir, imageFile);
                if (!Files.exists(imagePath)) {
                    sseService.pushAiResponse(sessionId, taskId, "❌ 图片文件不存在: " + imageFile, imageFile);
                    return;
                }
                
                byte[] imageBytes = Files.readAllBytes(imagePath);
                log.info("分析图片: {}, taskId={}", imageFile, taskId);
                
                String aiResponse = callAiWithImage(sessionId, message, imageBytes);
                sseService.pushAiResponse(sessionId, taskId, aiResponse, imageFile);
                
            } catch (Exception e) {
                log.error("分析图片失败: taskId={}, error={}", taskId, e.getMessage(), e);
                sseService.pushAiResponse(sessionId, taskId, "❌ 分析失败: " + e.getMessage(), imageFile);
            }
        });
        
        return taskId;
    }
    
    /**
     * 等待ESP32上传图片，返回图片文件
     */
    private File waitForImageFile(String clientId) {
        Path photosPath = Paths.get(photosDir);
        long startTime = System.currentTimeMillis();
        long latestModifiedBefore = getLatestImageTime(photosPath);
        
        while (System.currentTimeMillis() - startTime < maxWaitSeconds * 1000) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                File latestImage = getLatestImage(photosPath);
                if (latestImage != null && latestImage.lastModified() > latestModifiedBefore) {
                    return latestImage;
                }
            } catch (Exception e) {
                log.warn("等待图片时出错: {}", e.getMessage());
            }
        }
        return null;
    }
    
    private long getLatestImageTime(Path photosPath) {
        File latestImage = getLatestImage(photosPath);
        return latestImage != null ? latestImage.lastModified() : 0;
    }
    
    private File getLatestImage(Path photosPath) {
        File dir = photosPath.toFile();
        if (!dir.exists() || !dir.isDirectory()) return null;
        
        File[] files = dir.listFiles((d, name) -> 
                name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg"));
        
        if (files == null || files.length == 0) return null;
        
        return Arrays.stream(files)
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);
    }
    
    /**
     * 调用AI分析图片
     */
    private String callAiWithImage(String sessionId, String message, byte[] imageBytes) throws Exception {
        // 编码图片为Base64
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String imageUrl = "data:image/jpeg;base64," + base64Image;
        
        // 获取或创建会话历史
        List<Map<String, Object>> history = sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
        List<Map<String, String>> simple = simpleHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        // 构建用户消息（含图片）
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", List.of(
                Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)),
                Map.of("type", "text", "text", message)
        ));
        history.add(userMessage);
        simple.add(Map.of("role", "user", "content", message));
        
        // 构建请求
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "你是一个智能家居助手，负责分析摄像头拍摄的环境图片并回答用户问题。请用简洁的中文回答。"));
        messages.addAll(history);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        
        // 发送请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
        
        // 解析响应
        JsonNode root = objectMapper.readTree(response.getBody());
        String aiResponse = root.path("choices").get(0).path("message").path("content").asText();
        
        // 保存AI回复到历史
        history.add(Map.of("role", "assistant", "content", aiResponse));
        simple.add(Map.of("role", "assistant", "content", aiResponse));
        
        log.info("AI对话完成: sessionId={}", sessionId);
        return aiResponse;
    }
    
    @Override
    public List<Map<String, String>> getHistory(String sessionId) {
        return simpleHistory.getOrDefault(sessionId, new ArrayList<>());
    }
    
    @Override
    public void clearHistory(String sessionId) {
        sessionHistory.remove(sessionId);
        simpleHistory.remove(sessionId);
        log.info("清空会话历史: sessionId={}", sessionId);
    }
}
