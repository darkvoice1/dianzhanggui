package com.darkvoice1.dianzhanggui.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.tenant.model.Store;
import org.apache.ibatis.annotations.Mapper;

/** 提供门店数据的数据库访问能力。 */
@Mapper
public interface StoreMapper extends BaseMapper<Store> {
}
