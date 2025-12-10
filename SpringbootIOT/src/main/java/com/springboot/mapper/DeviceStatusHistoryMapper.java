package com.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springboot.pojo.DeviceStatusHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 设备状态历史Mapper
 */
@Mapper
public interface DeviceStatusHistoryMapper extends BaseMapper<DeviceStatusHistory> {
    
    /**
     * 获取指定设备的最近N条状态记录
     */
    @Select("SELECT * FROM device_status_history WHERE client_id = #{clientId} ORDER BY create_time DESC LIMIT #{limit}")
    List<DeviceStatusHistory> findLatestByClientId(@Param("clientId") String clientId, @Param("limit") int limit);
}
