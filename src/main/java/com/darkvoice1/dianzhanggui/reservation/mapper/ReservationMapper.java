package com.darkvoice1.dianzhanggui.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.reservation.model.Reservation;
import org.apache.ibatis.annotations.Mapper;

/** 提供预约记录的数据库访问能力。 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
}
