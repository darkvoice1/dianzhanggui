package com.darkvoice1.dianzhanggui.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.reservation.model.Reservation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/** 提供预约记录的数据库访问能力。 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    /** 在不存在有效预约时插入预约记录，并回填生成的预约主键。 */
    @Insert("""
            INSERT INTO reservation (merchant_id, product_availability_id, customer_profile_id, status)
            VALUES (#{merchantId}, #{productAvailabilityId}, #{customerProfileId}, #{status})
            ON CONFLICT (merchant_id, product_availability_id, customer_profile_id)
                WHERE status = 'RESERVED'
            DO NOTHING
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertReservedIfAbsent(Reservation reservation);

    /** 仅将仍有效且属于当前客户的预约更新为已取消状态。 */
    @Update("""
            UPDATE reservation
            SET status = 'CANCELLED',
                cancelled_at = #{cancelledAt}
            WHERE id = #{reservationId}
              AND merchant_id = #{merchantId}
              AND customer_profile_id = #{customerProfileId}
              AND status = 'RESERVED'
            """)
    int cancelIfReserved(@Param("merchantId") Long merchantId,
            @Param("customerProfileId") Long customerProfileId,
            @Param("reservationId") Long reservationId,
            @Param("cancelledAt") LocalDateTime cancelledAt);
}
