-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    client_id VARCHAR(64) NOT NULL COMMENT '设备ID',
    operation VARCHAR(32) NOT NULL COMMENT '操作类型(capture/led/led_brightness等)',
    operation_desc VARCHAR(255) NOT NULL COMMENT '操作描述(中文)',
    cmd_id BIGINT NOT NULL COMMENT '指令ID',
    result VARCHAR(16) NOT NULL COMMENT '执行结果(success/failed)',
    result_msg VARCHAR(512) COMMENT '结果消息(中文)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_client_id (client_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备操作日志表';
