package com.zfc.eldercare.core.vo;

import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 分页结果 VO。
 * 说明：PageInfo 继承 ArrayList，Jackson 会将其序列化为 JSON 数组而丢失分页元数据，
 * 故统一用本 VO 包装（详细设计文档 7.4 分页约定）。
 */
public record PageVO<T>(
        int pageNum,
        int pageSize,
        long total,
        int pages,
        List<T> list
) {
    public static <T> PageVO<T> of(PageInfo<T> pageInfo) {
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), pageInfo.getList());
    }
}
