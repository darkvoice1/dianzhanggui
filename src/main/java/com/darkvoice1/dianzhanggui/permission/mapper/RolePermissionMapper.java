package com.darkvoice1.dianzhanggui.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.permission.model.RolePermission;
import org.apache.ibatis.annotations.Mapper;

/** 提供角色权限关联关系的数据库访问能力。 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
