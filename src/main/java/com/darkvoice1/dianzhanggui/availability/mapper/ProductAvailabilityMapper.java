package com.darkvoice1.dianzhanggui.availability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.availability.model.ProductAvailability;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 提供商品可用性记录的数据库访问能力。 */
@Mapper
public interface ProductAvailabilityMapper extends BaseMapper<ProductAvailability> {

    /** 在剩余数量大于零时原子扣减当前商家的商品可用性数量。 */
    @Update("""
            UPDATE product_availability
            SET remaining_capacity = remaining_capacity - 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{availabilityId}
              AND merchant_id = #{merchantId}
              AND remaining_capacity > 0
            """)
    int decreaseRemainingCapacityIfAvailable(@Param("merchantId") Long merchantId,
            @Param("availabilityId") Long availabilityId);

    /** 在剩余数量未达到总数量时原子回补当前商家的商品可用性数量。 */
    @Update("""
            UPDATE product_availability
            SET remaining_capacity = remaining_capacity + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{availabilityId}
              AND merchant_id = #{merchantId}
              AND remaining_capacity < capacity
            """)
    int increaseRemainingCapacityIfRecoverable(@Param("merchantId") Long merchantId,
            @Param("availabilityId") Long availabilityId);
}
