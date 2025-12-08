package com.springboot.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("operation_log")
public class OperationLog {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 设备ID
     */
    private String clientId;
    
    /**
     * 操作类型(capture/led/led_brightness等)
     */
    private String operation;
    
    /**
     * 操作描述(中文)
     */
    private String operationDesc;
    
    /**
     * 指令ID
     */
    private Long cmdId;
    
    /**
     * 执行结果(success/failed)
     */
    private String result;
    
    /**
     * 结果消息(中文)
     */
    private String resultMsg;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
