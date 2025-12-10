package com.springboot.service.Impl;

import com.springboot.mapper.DeviceStatusHistoryMapper;
import com.springboot.pojo.DeviceStatusHistory;
import com.springboot.pojo.vo.StatusChartResponse;
import com.springboot.service.DeviceStatusHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 设备状态历史服务实现
 */
@Slf4j
@Service
public class DeviceStatusHistoryServiceImpl implements DeviceStatusHistoryService {
    
    @Autowired
    private DeviceStatusHistoryMapper mapper;
    
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    @Override
    public void save(String clientId, int rssi, int freeHeap, long uptime) {
        DeviceStatusHistory history = new DeviceStatusHistory();
        history.setClientId(clientId);
        history.setRssi(rssi);
        history.setFreeHeap(freeHeap);
        history.setUptime(uptime);
        history.setCreateTime(LocalDateTime.now());
        mapper.insert(history);
        log.debug("保存设备状态: clientId={}, rssi={}, freeHeap={}", clientId, rssi, freeHeap);
    }
    
    @Override
    public List<DeviceStatusHistory> getLatest(String clientId, int limit) {
        return mapper.findLatestByClientId(clientId, limit);
    }
    
    @Override
    public StatusChartResponse getChartData(String clientId, int limit) {
        List<DeviceStatusHistory> list = mapper.findLatestByClientId(clientId, limit);
        
        // 反转列表（数据库查询是DESC，图表需要ASC）
        Collections.reverse(list);
        
        List<String> labels = new ArrayList<>();
        List<Integer> rssiData = new ArrayList<>();
        List<Integer> freeHeapData = new ArrayList<>();
        
        for (DeviceStatusHistory h : list) {
            labels.add(h.getCreateTime().format(TIME_FORMAT));
            rssiData.add(h.getRssi());
            freeHeapData.add(h.getFreeHeap() / 1024);  // 转换为KB
        }
        
        return StatusChartResponse.builder()
                .labels(labels)
                .rssiData(rssiData)
                .freeHeapData(freeHeapData)
                .build();
    }
}
