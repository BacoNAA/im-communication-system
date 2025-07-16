package com.im.imcommunicationsystem.message.dto.response;

import com.im.imcommunicationsystem.message.dto.response.MessageDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 消息搜索响应对象
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSearchResponse {

    /**
     * 搜索结果列表
     */
    private List<MessageSearchResult> results;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer size;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 搜索耗时（毫秒）
     */
    private Long searchTime;

    /**
     * 聚合统计信息
     */
    private Map<String, Object> aggregations;

    /**
     * 搜索建议（当搜索结果为空时）
     */
    private List<String> suggestions;

    /**
     * 消息搜索结果项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageSearchResult {

        /**
         * 消息信息
         */
        private MessageDTO message;

        /**
         * 搜索评分
         */
        private Double score;

        /**
         * 高亮内容
         */
        private Map<String, List<String>> highlights;

        /**
         * 匹配的字段
         */
        private List<String> matchedFields;

        /**
         * 上下文消息（搜索结果前后的消息）
         */
        private List<MessageDTO> contextMessages;

        /**
         * 检查是否有高亮内容
         * 
         * @return 是否有高亮内容
         */
        public boolean hasHighlights() {
            return highlights != null && !highlights.isEmpty();
        }

        /**
         * 获取内容高亮
         * 
         * @return 内容高亮列表
         */
        public List<String> getContentHighlights() {
            if (highlights == null) {
                return null;
            }
            return highlights.get("content");
        }

        /**
         * 检查是否有上下文消息
         * 
         * @return 是否有上下文消息
         */
        public boolean hasContextMessages() {
            return contextMessages != null && !contextMessages.isEmpty();
        }

        /**
         * 获取高亮后的内容
         * 
         * @return 高亮后的内容
         */
        public String getHighlightedContent() {
            List<String> contentHighlights = getContentHighlights();
            if (contentHighlights != null && !contentHighlights.isEmpty()) {
                return contentHighlights.get(0);
            }
            
            return message != null ? message.getContent() : "";
        }
    }

    /**
     * 检查是否有搜索结果
     * 
     * @return 是否有搜索结果
     */
    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }

    /**
     * 检查是否为空结果
     * 
     * @return 是否为空结果
     */
    public boolean isEmpty() {
        return total == null || total == 0;
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
        
        if (page != null && totalPages != null) {
            return page < totalPages - 1;
        }
        
        return false;
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
        
        return page != null && page > 0;
    }

    /**
     * 获取结果数量
     * 
     * @return 结果数量
     */
    public int getResultCount() {
        return results != null ? results.size() : 0;
    }

    /**
     * 检查是否有聚合统计
     * 
     * @return 是否有聚合统计
     */
    public boolean hasAggregations() {
        return aggregations != null && !aggregations.isEmpty();
    }

    /**
     * 检查是否有搜索建议
     * 
     * @return 是否有搜索建议
     */
    public boolean hasSuggestions() {
        return suggestions != null && !suggestions.isEmpty();
    }

    /**
     * 获取搜索统计信息
     * 
     * @return 搜索统计信息
     */
    public String getSearchStats() {
        StringBuilder stats = new StringBuilder();
        
        if (total != null) {
            stats.append("找到 ").append(total).append(" 条结果");
        }
        
        if (searchTime != null) {
            stats.append("，耗时 ").append(searchTime).append(" 毫秒");
        }
        
        return stats.toString();
    }

    /**
     * 获取分页信息
     * 
     * @return 分页信息
     */
    public String getPaginationInfo() {
        if (page == null || size == null || total == null) {
            return "";
        }
        
        int start = page * size + 1;
        int end = Math.min((page + 1) * size, total.intValue());
        
        return String.format("第 %d-%d 条，共 %d 条", start, end, total);
    }
}