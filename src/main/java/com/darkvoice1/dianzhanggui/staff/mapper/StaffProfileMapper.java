package com.darkvoice1.dianzhanggui.staff.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.staff.model.StaffProfile;
import org.apache.ibatis.annotations.Mapper;

/** 提供人员档案的数据库访问能力。 */
@Mapper
public interface StaffProfileMapper extends BaseMapper<StaffProfile> {
}
