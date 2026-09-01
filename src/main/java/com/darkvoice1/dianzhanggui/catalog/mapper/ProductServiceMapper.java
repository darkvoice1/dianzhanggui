package com.darkvoice1.dianzhanggui.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.catalog.model.ProductService;
import org.apache.ibatis.annotations.Mapper;

/** 提供商品与服务目录的数据库访问能力。 */
@Mapper
public interface ProductServiceMapper extends BaseMapper<ProductService> {
}
