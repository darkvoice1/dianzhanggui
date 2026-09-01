package com.darkvoice1.dianzhanggui.common.page;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证通用分页参数和分页结果模型。 */
class PageQueryTest {

    private static Validator validator;

    /** 创建参数校验器。 */
    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /** 释放参数校验器资源。 */
    @AfterAll
    static void closeValidator() {
        validator = null;
    }

    /** 验证分页参数拥有合理的默认值和偏移量。 */
    @Test
    void shouldUseDefaultPageValues() {
        PageQuery query = new PageQuery();

        assertEquals(1, query.getPage());
        assertEquals(20, query.getSize());
        assertEquals(0, query.offset());

        query.setPage(3);
        query.setSize(10);
        assertEquals(20, query.offset());
    }

    /** 验证页码和每页条数超出边界时校验失败。 */
    @Test
    void shouldRejectInvalidPageValues() {
        PageQuery query = new PageQuery();
        query.setPage(0);
        query.setSize(101);

        assertEquals(2, validator.validate(query).size());
    }

    /** 验证分页结果可以计算总页数并保护数据列表。 */
    @Test
    void shouldCalculateTotalPages() {
        PageResult<String> result = new PageResult<>(List.of("a", "b"), 21, 1, 10);

        assertEquals(3, result.totalPages());
        assertTrue(result.records().contains("a"));
    }
}
