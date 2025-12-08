package com.springboot.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DHT温湿度数据推送VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DhtDataVo {
    private String clientId;
    private Double temperature;
    private Double humidity;
    private String time;
}
