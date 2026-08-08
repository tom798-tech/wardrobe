package com.tom.wardrobe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tom.wardrobe.entity.OrderOutboxEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderOutboxMapper extends BaseMapper<OrderOutboxEvent> {
}
