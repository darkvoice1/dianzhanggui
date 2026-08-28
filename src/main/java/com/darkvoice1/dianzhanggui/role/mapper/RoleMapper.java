package com.darkvoice1.dianzhanggui.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.role.model.Role;
import org.apache.ibatis.annotations.Mapper;

/** 提供角色定义的数据库访问能力。 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
