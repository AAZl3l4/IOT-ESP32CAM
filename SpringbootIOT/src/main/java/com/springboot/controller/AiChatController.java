package com.springboot.controller;

import com.springboot.service.AiChatService;
import com.springboot.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI对话控制器
 * 提供Qwen-VL视觉问答API（异步+SSE推送）
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@CrossOrigin(origins = "*")
public class AiChatController {
    
    @Autowired
    private AiChatService aiChatService;
    
    /**
     * 发送消息并获取AI回答（异步，自动带图片，结果通过SSE推送）
     * @param clientId 设备ID
     * @param request 包含sessionId和message
     * @return taskId 任务ID，用于匹配SSE推送的结果
     */
    @PostMapping("/chat/{clientId}")
    public Result<Map<String, String>> chat(
            @PathVariable String clientId,
            @RequestBody Map<String, String> request) {
        
        String sessionId = request.getOrDefault("sessionId", "default");
        String message = request.get("message");
        
        if (message == null || message.trim().isEmpty()) {
            return Result.error("消息不能为空");
        }
        
        log.info("AI对话请求(异步): clientId={}, sessionId={}, message={}", clientId, sessionId, message);
        
        // 异步执行，立即返回taskId
        String taskId = aiChatService.chatWithCaptureAsync(clientId, sessionId, message);
        
        return Result.success("已提交", Map.of(
                "taskId", taskId,
                "sessionId", sessionId
        ));
    }
    
    /**
     * 分析指定图片（异步，结果通过SSE推送）
     */
    @PostMapping("/analyze")
    public Result<Map<String, String>> analyzeImage(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "default");
        String imageFile = request.get("imageFile");
        String message = request.getOrDefault("message", "请描述这张图片");
        
        if (imageFile == null || imageFile.trim().isEmpty()) {
            return Result.error("图片文件名不能为空");
        }
        
        log.info("AI分析图片请求(异步): imageFile={}", imageFile);
        
        // 异步执行，立即返回taskId
        String taskId = aiChatService.analyzeImageAsync(sessionId, imageFile, message);
        
        return Result.success("已提交", Map.of(
                "taskId", taskId,
                "imageFile", imageFile
        ));
    }
    
    /**
     * 获取对话历史
     */
    @GetMapping("/history/{sessionId}")
    public Result<List<Map<String, String>>> getHistory(@PathVariable String sessionId) {
        List<Map<String, String>> history = aiChatService.getHistory(sessionId);
        return Result.success("成功", history);
    }
    
    /**
     * 清空对话历史
     */
    @DeleteMapping("/history/{sessionId}")
    public Result<Void> clearHistory(@PathVariable String sessionId) {
        aiChatService.clearHistory(sessionId);
        return Result.success("已清空对话历史", null);
    }
}
