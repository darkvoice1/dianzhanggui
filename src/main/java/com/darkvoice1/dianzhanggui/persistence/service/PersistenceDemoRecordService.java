package com.darkvoice1.dianzhanggui.persistence.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.common.tenant.TenantContext;
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
        record.setMerchantId(TenantContext.requireMerchantId());
        recordMapper.insert(record);
        return record;
    }

    /** 根据主键查询示例记录。 */
    public PersistenceDemoRecord findById(Long id) {
        Long merchantId = TenantContext.requireMerchantId();
        PersistenceDemoRecord record = recordMapper.selectOne(new LambdaQueryWrapper<PersistenceDemoRecord>()
                .eq(PersistenceDemoRecord::getId, id)
                .eq(PersistenceDemoRecord::getMerchantId, merchantId));
        if (record == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return record;
    }

    /** 在当前商家范围内修改指定记录名称。 */
    public PersistenceDemoRecord update(Long id, String name) {
        Long merchantId = TenantContext.requireMerchantId();
        int updatedRows = recordMapper.update(null, new LambdaUpdateWrapper<PersistenceDemoRecord>()
                .set(PersistenceDemoRecord::getName, name)
                .eq(PersistenceDemoRecord::getId, id)
                .eq(PersistenceDemoRecord::getMerchantId, merchantId));
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return findById(id);
    }

    /** 在当前商家范围内删除指定记录。 */
    public void delete(Long id) {
        Long merchantId = TenantContext.requireMerchantId();
        int deletedRows = recordMapper.delete(new LambdaQueryWrapper<PersistenceDemoRecord>()
                .eq(PersistenceDemoRecord::getId, id)
                .eq(PersistenceDemoRecord::getMerchantId, merchantId));
        if (deletedRows == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}
