package com.darkvoice1.dianzhanggui.availability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.availability.model.ProductAvailability;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 提供商品可用性记录的数据库访问能力。 */
@Mapper
public interface ProductAvailabilityMapper extends BaseMapper<ProductAvailability> {

    /**
     * 使用条件更新原子扣减数量；PostgreSQL 会协调同一行的并发更新，剩余数量是最终防超卖条件。
     */
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

    /**
     * 使用条件更新原子回补数量；避免并发取消或异常数据使剩余数量超过总数量。
     */
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
