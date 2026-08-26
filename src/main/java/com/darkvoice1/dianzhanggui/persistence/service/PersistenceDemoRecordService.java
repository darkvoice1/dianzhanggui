package com.darkvoice1.dianzhanggui.persistence.service;

import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.infrastructure.exception.BusinessException;
import com.darkvoice1.dianzhanggui.persistence.mapper.PersistenceDemoRecordMapper;
import com.darkvoice1.dianzhanggui.persistence.model.PersistenceDemoRecord;
import org.springframework.stereotype.Service;

/** 提供示例记录的持久化业务能力。 */
@Service
public class PersistenceDemoRecordService {

    private final PersistenceDemoRecordMapper recordMapper;

    /** 创建示例记录服务并注入数据库访问对象。 */
    public PersistenceDemoRecordService(PersistenceDemoRecordMapper recordMapper) {
        this.recordMapper = recordMapper;
    }

    /** 保存一条示例记录并返回生成后的主键。 */
    public PersistenceDemoRecord create(String name) {
        PersistenceDemoRecord record = new PersistenceDemoRecord();
        record.setName(name);
        recordMapper.insert(record);
        return record;
    }

    /** 根据主键查询示例记录。 */
    public PersistenceDemoRecord findById(Long id) {
        PersistenceDemoRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return record;
    }
}
