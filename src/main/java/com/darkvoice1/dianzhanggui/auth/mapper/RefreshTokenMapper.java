package com.darkvoice1.dianzhanggui.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.auth.model.RefreshToken;
import org.apache.ibatis.annotations.Mapper;

/** 提供刷新令牌的数据库访问能力。 */
@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {
}
