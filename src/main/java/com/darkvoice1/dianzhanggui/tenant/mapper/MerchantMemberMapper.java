package com.darkvoice1.dianzhanggui.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.tenant.model.MerchantMember;
import org.apache.ibatis.annotations.Mapper;

/** 提供商家成员关系的数据库访问能力。 */
@Mapper
public interface MerchantMemberMapper extends BaseMapper<MerchantMember> {
}
