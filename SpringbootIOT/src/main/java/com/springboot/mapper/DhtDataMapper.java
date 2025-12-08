package com.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springboot.pojo.DhtData;
import org.apache.ibatis.annotations.Mapper;

/**
 * DHT22温湿度数据Mapper
 */
@Mapper
public interface DhtDataMapper extends BaseMapper<DhtData> {
}
