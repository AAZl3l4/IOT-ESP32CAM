package com.springboot.service;

import java.util.List;
import java.util.Map;

/**
 * AI对话服务接口
 * 集成Qwen-VL视觉语言模型
 */
public interface AiChatService {
    
    /**
     * 异步发送消息并获取AI回答（自动拍照）
     * 立即返回taskId，完成后通过SSE推送结果
     * @param clientId 设备ID（用于拍照）
     * @param sessionId 会话ID（用于记忆）
     * @param message 用户消息
     * @return taskId 任务ID
     */
    String chatWithCaptureAsync(String clientId, String sessionId, String message);
    
    /**
     * 异步分析指定图片
     * 立即返回taskId，完成后通过SSE推送结果
     * @param sessionId 会话ID
     * @param imageFile 图片文件名
     * @param message 用户消息
     * @return taskId 任务ID
     */
    String analyzeImageAsync(String sessionId, String imageFile, String message);
    
    /**
     * 获取对话历史
     * @param sessionId 会话ID
     * @return 对话历史列表
     */
    List<Map<String, String>> getHistory(String sessionId);
    
    /**
     * 清空对话历史
     * @param sessionId 会话ID
     */
    void clearHistory(String sessionId);
}
