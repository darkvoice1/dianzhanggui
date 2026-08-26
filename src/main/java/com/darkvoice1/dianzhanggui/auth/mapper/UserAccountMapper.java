package com.darkvoice1.dianzhanggui.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.auth.model.UserAccount;
import org.apache.ibatis.annotations.Mapper;

/** 提供用户账号的数据库访问能力。 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
}
