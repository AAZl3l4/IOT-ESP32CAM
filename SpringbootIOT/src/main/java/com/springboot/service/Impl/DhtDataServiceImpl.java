package com.springboot.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.springboot.mapper.DhtDataMapper;
import com.springboot.pojo.DhtData;
import com.springboot.service.DhtDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DHT22温湿度数据服务实现
 */
@Slf4j
@Service
public class DhtDataServiceImpl implements DhtDataService {
    
    @Autowired
    private DhtDataMapper dhtDataMapper;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    @Override
    public void save(String clientId, double temperature, double humidity) {
        DhtData data = new DhtData();
        data.setClientId(clientId);
        data.setTemperature(BigDecimal.valueOf(temperature));
        data.setHumidity(BigDecimal.valueOf(humidity));
        data.setCreateTime(LocalDateTime.now());
        
        dhtDataMapper.insert(data);
        log.info("保存温湿度数据: clientId={}, 温度={}℃, 湿度={}%", clientId, temperature, humidity);
    }
    
    @Override
    public DhtData getLatest(String clientId) {
        LambdaQueryWrapper<DhtData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DhtData::getClientId, clientId);
        wrapper.orderByDesc(DhtData::getCreateTime);
        wrapper.last("LIMIT 1");
        return dhtDataMapper.selectOne(wrapper);
    }
    
    @Override
    public List<DhtData> getLatestList(String clientId, int limit) {
        LambdaQueryWrapper<DhtData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DhtData::getClientId, clientId);
        wrapper.orderByDesc(DhtData::getCreateTime);
        wrapper.last("LIMIT " + limit);
        List<DhtData> list = dhtDataMapper.selectList(wrapper);
        // 反转顺序，让时间从旧到新
        Collections.reverse(list);
        return list;
    }
    
    @Override
    public Map<String, Object> getDashboardData(String clientId, int chartLimit) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取最新数据
        DhtData latest = getLatest(clientId);
        if (latest != null) {
            result.put("hasData", true);
            result.put("temperature", latest.getTemperature());
            result.put("humidity", latest.getHumidity());
            result.put("updateTime", latest.getCreateTime().format(TIME_FORMATTER));
        } else {
            result.put("hasData", false);
        }
        
        // 获取图表数据
        List<DhtData> chartData = getLatestList(clientId, chartLimit);
        List<String> labels = new ArrayList<>();
        List<BigDecimal> temperatures = new ArrayList<>();
        List<BigDecimal> humidities = new ArrayList<>();
        
        for (DhtData data : chartData) {
            labels.add(data.getCreateTime().format(TIME_FORMATTER));
            temperatures.add(data.getTemperature());
            humidities.add(data.getHumidity());
        }
        
        result.put("labels", labels);
        result.put("temperatures", temperatures);
        result.put("humidities", humidities);
        
        return result;
    }
}
