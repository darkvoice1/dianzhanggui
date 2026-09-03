package com.darkvoice1.dianzhanggui.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.reservation.model.Reservation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

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
}
