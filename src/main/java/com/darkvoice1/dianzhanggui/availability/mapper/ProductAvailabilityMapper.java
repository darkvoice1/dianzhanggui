package com.darkvoice1.dianzhanggui.availability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.availability.model.ProductAvailability;
import org.apache.ibatis.annotations.Mapper;

/** 提供商品可用性记录的数据库访问能力。 */
@Mapper
public interface ProductAvailabilityMapper extends BaseMapper<ProductAvailability> {
}
