package com.im.imcommunicationsystem.message.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页响应对象
 * 
 * @param <T> 数据类型
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /**
     * 数据列表
     */
    private List<T> content;

    /**
     * 当前页码（从0开始）
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer size;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 是否为第一页
     */
    private Boolean first;

    /**
     * 是否为最后一页
     */
    private Boolean last;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 当前页记录数
     */
    private Integer numberOfElements;

    /**
     * 是否为空页
     */
    private Boolean empty;

    /**
     * 排序信息
     */
    private String sort;

    /**
     * 创建分页响应
     * 
     * @param content 数据列表
     * @param page 当前页码
     * @param size 每页大小
     * @param total 总记录数
     * @param <T> 数据类型
     * @return 分页响应
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long total) {
        int totalPages = (int) Math.ceil((double) total / size);
        
        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .total(total)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .numberOfElements(content != null ? content.size() : 0)
                .empty(content == null || content.isEmpty())
                .build();
    }

    /**
     * 创建空分页响应
     * 
     * @param page 当前页码
     * @param size 每页大小
     * @param <T> 数据类型
     * @return 空分页响应
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return of(List.of(), page, size, 0L);
    }

    /**
     * 检查是否有数据
     * 
     * @return 是否有数据
     */
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }

    /**
     * 检查是否为空
     * 
     * @return 是否为空
     */
    public boolean isEmpty() {
        if (empty != null) {
            return empty;
        }
        return content == null || content.isEmpty();
    }

    /**
     * 检查是否为第一页
     * 
     * @return 是否为第一页
     */
    public boolean isFirst() {
        if (first != null) {
            return first;
        }
        return page != null && page == 0;
    }

    /**
     * 检查是否为最后一页
     * 
     * @return 是否为最后一页
     */
    public boolean isLast() {
        if (last != null) {
            return last;
        }
        if (page != null && totalPages != null) {
            return page >= totalPages - 1;
        }
        return false;
    }

    /**
     * 检查是否有下一页
     * 
     * @return 是否有下一页
     */
    public boolean hasNext() {
        if (hasNext != null) {
            return hasNext;
        }
        return !isLast();
    }

    /**
     * 检查是否有上一页
     * 
     * @return 是否有上一页
     */
    public boolean hasPrevious() {
        if (hasPrevious != null) {
            return hasPrevious;
        }
        return !isFirst();
    }

    /**
     * 获取下一页页码
     * 
     * @return 下一页页码
     */
    public Integer getNextPage() {
        return hasNext() ? page + 1 : null;
    }

    /**
     * 获取上一页页码
     * 
     * @return 上一页页码
     */
    public Integer getPreviousPage() {
        return hasPrevious() ? page - 1 : null;
    }

    /**
     * 获取当前页记录数
     * 
     * @return 当前页记录数
     */
    public int getNumberOfElements() {
        if (numberOfElements != null) {
            return numberOfElements;
        }
        return content != null ? content.size() : 0;
    }

    /**
     * 获取分页信息描述
     * 
     * @return 分页信息描述
     */
    public String getPageInfo() {
        if (total == null || page == null || size == null) {
            return "";
        }
        
        if (total == 0) {
            return "暂无数据";
        }
        
        int start = page * size + 1;
        int end = Math.min((page + 1) * size, total.intValue());
        
        return String.format("第 %d-%d 条，共 %d 条", start, end, total);
    }

    /**
     * 获取分页统计信息
     * 
     * @return 分页统计信息
     */
    public String getPageStats() {
        if (totalPages == null || page == null) {
            return "";
        }
        
        return String.format("第 %d 页，共 %d 页", page + 1, totalPages);
    }
}