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

-- 设备状态历史表（存储rssi和freeHeap用于折线图展示）
CREATE TABLE IF NOT EXISTS device_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    client_id VARCHAR(64) NOT NULL COMMENT '设备ID',
    rssi INT NOT NULL COMMENT 'WiFi信号强度(dBm)',
    free_heap INT NOT NULL COMMENT '空闲内存(bytes)',
    uptime BIGINT NOT NULL COMMENT '运行时间(秒)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_client_id (client_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备状态历史表';

-- 自动化配置表
CREATE TABLE IF NOT EXISTS automation_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    client_id VARCHAR(64) NOT NULL UNIQUE COMMENT '设备ID',
    enabled BOOLEAN DEFAULT TRUE COMMENT '自动化总开关',
    temp_high INT DEFAULT 25 COMMENT '高温阈值(℃)',
    temp_low INT DEFAULT 17 COMMENT '低温阈值(℃)',
    humid_high INT DEFAULT 90 COMMENT '高湿阈值(%)',
    humid_low INT DEFAULT 45 COMMENT '低湿阈值(%)',
    memory_threshold INT DEFAULT 51200 COMMENT '内存阈值(bytes)',
    rssi_threshold INT DEFAULT -75 COMMENT '信号阈值(dBm)',
    manual_pause_ms BIGINT DEFAULT 300000 COMMENT '手动操作后暂停时间(ms)',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化配置表';

-- 温湿度和光照数据表
CREATE TABLE IF NOT EXISTS dht_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    client_id VARCHAR(64) NOT NULL COMMENT '设备ID',
    temperature DECIMAL(5,2) NOT NULL COMMENT '温度(℃)',
    humidity DECIMAL(5,2) NOT NULL COMMENT '湿度(%)',
    light_dark TINYINT(1) DEFAULT NULL COMMENT '是否暗(1=暗,0=亮)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
    INDEX idx_client_id (client_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DHT22温湿度和光照数据表';
