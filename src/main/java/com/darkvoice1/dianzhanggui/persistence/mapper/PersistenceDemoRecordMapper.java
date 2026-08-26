package com.darkvoice1.dianzhanggui.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.dianzhanggui.persistence.model.PersistenceDemoRecord;
import org.apache.ibatis.annotations.Mapper;

/** 提供示例记录的数据库访问能力。 */
@Mapper
public interface PersistenceDemoRecordMapper extends BaseMapper<PersistenceDemoRecord> {
}
