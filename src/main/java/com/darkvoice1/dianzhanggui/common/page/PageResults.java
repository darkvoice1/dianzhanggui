package com.darkvoice1.dianzhanggui.common.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/** 提供 MyBatis-Plus 分页对象到统一分页结果的转换方法。 */
public final class PageResults {

    private PageResults() {
    }

    /** 将 MyBatis-Plus 分页结果转换为项目统一的分页结果。 */
    public static <T> PageResult<T> from(Page<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(),
                Math.toIntExact(page.getCurrent()), Math.toIntExact(page.getSize()));
    }
}
