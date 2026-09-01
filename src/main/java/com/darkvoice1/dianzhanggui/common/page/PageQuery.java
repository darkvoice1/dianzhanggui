package com.darkvoice1.dianzhanggui.common.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 表示列表接口通用的分页查询参数。 */
public class PageQuery {

    /** 默认页码。 */
    public static final int DEFAULT_PAGE = 1;

    /** 默认每页条数。 */
    public static final int DEFAULT_SIZE = 20;

    /** 允许的最大每页条数。 */
    public static final int MAX_SIZE = 100;

    /** 当前页码，从 1 开始。 */
    @Min(value = 1, message = "页码必须大于等于 1")
    @Max(value = 1_000_000, message = "页码不能超过 1000000")
    private int page = DEFAULT_PAGE;

    /** 每页返回条数。 */
    @Min(value = 1, message = "每页条数必须大于等于 1")
    @Max(value = MAX_SIZE, message = "每页条数不能超过 100")
    private int size = DEFAULT_SIZE;

    /** 获取当前页码。 */
    public int getPage() {
        return page;
    }

    /** 设置当前页码。 */
    public void setPage(int page) {
        this.page = page;
    }

    /** 获取每页条数。 */
    public int getSize() {
        return size;
    }

    /** 设置每页条数。 */
    public void setSize(int size) {
        this.size = size;
    }

    /** 计算数据库查询使用的偏移量。 */
    public long offset() {
        return (long) (page - 1) * size;
    }
}
