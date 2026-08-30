package com.darkvoice1.dianzhanggui.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.customer.model.CustomerProfile;
import org.apache.ibatis.annotations.Mapper;

/** 提供客户档案的数据库访问能力。 */
@Mapper
public interface CustomerProfileMapper extends BaseMapper<CustomerProfile> {
}
