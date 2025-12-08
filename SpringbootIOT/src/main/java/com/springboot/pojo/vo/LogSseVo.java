package com.springboot.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 操作日志SSE推送VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogSseVo {
    private String clientId;
    private String operation;
    private String operationDesc;
    private String result;
    private String resultMsg;
    private String time;
}
