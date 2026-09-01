package com.darkvoice1.dianzhanggui.common.page;

import java.util.List;

/** 表示列表接口统一返回的分页结果。 */
public record PageResult<T>(List<T> records, long total, int page, int size) {

    /** 创建分页结果并复制数据列表，避免调用方修改内部结果。 */
    public PageResult {
        records = List.copyOf(records);
    }

    /** 计算总页数，空结果返回 0。 */
    public long totalPages() {
        return total == 0 ? 0 : (total + size - 1) / size;
    }
}
