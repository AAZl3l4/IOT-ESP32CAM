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
