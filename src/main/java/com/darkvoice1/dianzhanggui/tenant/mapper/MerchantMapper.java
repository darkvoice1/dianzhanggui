package com.darkvoice1.dianzhanggui.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.tenant.model.Merchant;
import org.apache.ibatis.annotations.Mapper;

/** 提供商家数据的数据库访问能力。 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {
}
