package com.darkvoice1.dianzhanggui.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.permission.model.Permission;
import org.apache.ibatis.annotations.Mapper;

/** 提供权限定义的数据库访问能力。 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}
