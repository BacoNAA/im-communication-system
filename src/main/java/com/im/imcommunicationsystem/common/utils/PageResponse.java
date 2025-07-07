package com.im.imcommunicationsystem.common.utils;

import lombok.Data;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * 统一分页响应封装类
 * @param <T> 分页数据的泛型
 */
@Data
public class PageResponse<T> {

    /** 当前页码 (从1开始) */
    private int pageNum;

    /** 每页数量 */
    private int pageSize;

    /** 总页数 */
    private int totalPages;

    /** 总条目数 */
    private long totalSize;

    /** 当前页的数据列表 */
    private List<T> content;

    /**
     * 将 Spring Data JPA 的 Page 对象转换为我们自定义的 PageResponse 对象
     * @param page Spring Data JPA 的 Page 对象
     * @return 自定义的 PageResponse 对象
     * @param <T> 数据的泛型
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        PageResponse<T> response = new PageResponse<>();
        response.setPageNum(page.getNumber() + 1); // JPA Page number is 0-based
        response.setPageSize(page.getSize());
        response.setTotalPages(page.getTotalPages());
        response.setTotalSize(page.getTotalElements());
        response.setContent(page.getContent());
        return response;
    }
}