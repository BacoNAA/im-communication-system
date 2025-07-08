package com.im.imcommunicationsystem.common.utils;

import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页工具类
 * 提供分页查询和分页结果处理功能
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
public class PageUtils {

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE = 1;

    /**
     * 默认每页大小
     */
    public static final int DEFAULT_SIZE = 10;

    /**
     * 最大每页大小
     */
    public static final int MAX_SIZE = 100;

    /**
     * 最小每页大小
     */
    public static final int MIN_SIZE = 1;

    /**
     * 创建分页请求对象
     * 
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return Pageable对象
     */
    public static Pageable createPageable(Integer page, Integer size) {
        return createPageable(page, size, null);
    }

    /**
     * 创建分页请求对象（带排序）
     * 
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @param sort 排序对象
     * @return Pageable对象
     */
    public static Pageable createPageable(Integer page, Integer size, Sort sort) {
        // 参数校验和默认值设置
        int validPage = validatePage(page);
        int validSize = validateSize(size);
        
        // Spring Data JPA的页码从0开始，所以需要减1
        int pageIndex = validPage - 1;
        
        if (sort != null) {
            return PageRequest.of(pageIndex, validSize, sort);
        } else {
            return PageRequest.of(pageIndex, validSize);
        }
    }

    /**
     * 创建排序对象（升序）
     * 
     * @param properties 排序字段
     * @return Sort对象
     */
    public static Sort createSortAsc(String... properties) {
        if (properties == null || properties.length == 0) {
            return Sort.unsorted();
        }
        return Sort.by(Sort.Direction.ASC, properties);
    }

    /**
     * 创建排序对象（降序）
     * 
     * @param properties 排序字段
     * @return Sort对象
     */
    public static Sort createSortDesc(String... properties) {
        if (properties == null || properties.length == 0) {
            return Sort.unsorted();
        }
        return Sort.by(Sort.Direction.DESC, properties);
    }

    /**
     * 创建复合排序对象
     * 
     * @param sorts 排序对象数组
     * @return 复合Sort对象
     */
    public static Sort createSort(Sort... sorts) {
        if (sorts == null || sorts.length == 0) {
            return Sort.unsorted();
        }
        
        Sort result = sorts[0];
        for (int i = 1; i < sorts.length; i++) {
            result = result.and(sorts[i]);
        }
        return result;
    }

    /**
     * 解析排序字符串
     * 格式："field1:asc,field2:desc,field3"
     * 
     * @param sortStr 排序字符串
     * @return Sort对象
     */
    public static Sort parseSort(String sortStr) {
        if (!StringUtils.hasText(sortStr)) {
            return Sort.unsorted();
        }
        
        List<Sort.Order> orders = new ArrayList<>();
        String[] sortFields = sortStr.split(",");
        
        for (String field : sortFields) {
            field = field.trim();
            if (StringUtils.hasText(field)) {
                String[] parts = field.split(":");
                String property = parts[0].trim();
                
                if (StringUtils.hasText(property)) {
                    Sort.Direction direction = Sort.Direction.ASC; // 默认升序
                    
                    if (parts.length > 1) {
                        String directionStr = parts[1].trim().toLowerCase();
                        if ("desc".equals(directionStr) || "descending".equals(directionStr)) {
                            direction = Sort.Direction.DESC;
                        }
                    }
                    
                    orders.add(new Sort.Order(direction, property));
                }
            }
        }
        
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    /**
     * 校验页码
     * 
     * @param page 页码
     * @return 有效的页码
     */
    public static int validatePage(Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    /**
     * 校验每页大小
     * 
     * @param size 每页大小
     * @return 有效的每页大小
     */
    public static int validateSize(Integer size) {
        if (size == null || size < MIN_SIZE) {
            return DEFAULT_SIZE;
        }
        if (size > MAX_SIZE) {
            return MAX_SIZE;
        }
        return size;
    }

    /**
     * 转换分页结果
     * 
     * @param page 原始分页结果
     * @param mapper 转换函数
     * @param <T> 原始类型
     * @param <R> 目标类型
     * @return 转换后的分页结果
     */
    public static <T, R> PageResult<R> convertPage(Page<T> page, Function<T, R> mapper) {
        if (page == null) {
            return PageResult.empty();
        }
        
        List<R> convertedContent = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());
        
        return PageResult.<R>builder()
                .content(convertedContent)
                .page(page.getNumber() + 1) // 转换为从1开始的页码
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * 创建分页结果
     * 
     * @param content 内容列表
     * @param page 页码
     * @param size 每页大小
     * @param totalElements 总元素数
     * @param <T> 内容类型
     * @return 分页结果
     */
    public static <T> PageResult<T> createPageResult(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean isFirst = page == 1;
        boolean isLast = page >= totalPages;
        boolean hasNext = page < totalPages;
        boolean hasPrevious = page > 1;
        
        return PageResult.<T>builder()
                .content(content != null ? content : new ArrayList<>())
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(isFirst)
                .last(isLast)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }

    /**
     * 计算偏移量
     * 
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 偏移量
     */
    public static long calculateOffset(int page, int size) {
        return (long) (page - 1) * size;
    }

    /**
     * 计算总页数
     * 
     * @param totalElements 总元素数
     * @param size 每页大小
     * @return 总页数
     */
    public static int calculateTotalPages(long totalElements, int size) {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }

    /**
     * 获取分页范围信息
     * 
     * @param page 页码
     * @param size 每页大小
     * @param totalElements 总元素数
     * @return 分页范围信息
     */
    public static PageRange getPageRange(int page, int size, long totalElements) {
        long startIndex = calculateOffset(page, size) + 1;
        long endIndex = Math.min(startIndex + size - 1, totalElements);
        
        if (totalElements == 0) {
            startIndex = 0;
            endIndex = 0;
        }
        
        return new PageRange(startIndex, endIndex, totalElements);
    }

    /**
     * 分页范围信息
     */
    @Data
    public static class PageRange {
        /**
         * 起始索引（从1开始）
         */
        private final long startIndex;
        
        /**
         * 结束索引
         */
        private final long endIndex;
        
        /**
         * 总元素数
         */
        private final long totalElements;
        
        public PageRange(long startIndex, long endIndex, long totalElements) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.totalElements = totalElements;
        }
        
        /**
         * 获取范围描述
         * 
         * @return 范围描述字符串
         */
        public String getDescription() {
            if (totalElements == 0) {
                return "0 条记录";
            }
            return String.format("第 %d-%d 条，共 %d 条记录", startIndex, endIndex, totalElements);
        }
    }

    /**
     * 分页结果封装类
     * 
     * @param <T> 内容类型
     */
    @Data
    public static class PageResult<T> {
        /**
         * 内容列表
         */
        private List<T> content;
        
        /**
         * 当前页码（从1开始）
         */
        private int page;
        
        /**
         * 每页大小
         */
        private int size;
        
        /**
         * 总元素数
         */
        private long totalElements;
        
        /**
         * 总页数
         */
        private int totalPages;
        
        /**
         * 是否为第一页
         */
        private boolean first;
        
        /**
         * 是否为最后一页
         */
        private boolean last;
        
        /**
         * 是否有下一页
         */
        private boolean hasNext;
        
        /**
         * 是否有上一页
         */
        private boolean hasPrevious;
        
        /**
         * 分页范围信息
         */
        private PageRange pageRange;
        
        /**
         * 构建器
         * 
         * @param <T> 内容类型
         * @return 构建器实例
         */
        public static <T> PageResultBuilder<T> builder() {
            return new PageResultBuilder<>();
        }
        
        /**
         * 创建空的分页结果
         * 
         * @param <T> 内容类型
         * @return 空的分页结果
         */
        public static <T> PageResult<T> empty() {
            return PageResult.<T>builder()
                    .content(new ArrayList<>())
                    .page(1)
                    .size(DEFAULT_SIZE)
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();
        }
        
        /**
         * 获取分页范围信息
         * 
         * @return 分页范围信息
         */
        public PageRange getPageRange() {
            if (pageRange == null) {
                pageRange = PageUtils.getPageRange(page, size, totalElements);
            }
            return pageRange;
        }
        
        /**
         * 是否为空结果
         * 
         * @return 是否为空
         */
        public boolean isEmpty() {
            return content == null || content.isEmpty();
        }
        
        /**
         * 获取内容数量
         * 
         * @return 内容数量
         */
        public int getContentSize() {
            return content != null ? content.size() : 0;
        }
    }

    /**
     * 分页结果构建器
     * 
     * @param <T> 内容类型
     */
    public static class PageResultBuilder<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
        private boolean hasNext;
        private boolean hasPrevious;
        
        public PageResultBuilder<T> content(List<T> content) {
            this.content = content;
            return this;
        }
        
        public PageResultBuilder<T> page(int page) {
            this.page = page;
            return this;
        }
        
        public PageResultBuilder<T> size(int size) {
            this.size = size;
            return this;
        }
        
        public PageResultBuilder<T> totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }
        
        public PageResultBuilder<T> totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }
        
        public PageResultBuilder<T> first(boolean first) {
            this.first = first;
            return this;
        }
        
        public PageResultBuilder<T> last(boolean last) {
            this.last = last;
            return this;
        }
        
        public PageResultBuilder<T> hasNext(boolean hasNext) {
            this.hasNext = hasNext;
            return this;
        }
        
        public PageResultBuilder<T> hasPrevious(boolean hasPrevious) {
            this.hasPrevious = hasPrevious;
            return this;
        }
        
        public PageResult<T> build() {
            PageResult<T> result = new PageResult<>();
            result.content = this.content;
            result.page = this.page;
            result.size = this.size;
            result.totalElements = this.totalElements;
            result.totalPages = this.totalPages;
            result.first = this.first;
            result.last = this.last;
            result.hasNext = this.hasNext;
            result.hasPrevious = this.hasPrevious;
            return result;
        }
    }

    /**
     * 分页工具使用说明：
     * 
     * 1. 创建分页请求：
     *    - createPageable() - 创建基础分页请求
     *    - createSortAsc() - 创建升序排序
     *    - createSortDesc() - 创建降序排序
     *    - parseSort() - 解析排序字符串
     * 
     * 2. 参数校验：
     *    - validatePage() - 校验页码
     *    - validateSize() - 校验每页大小
     * 
     * 3. 结果转换：
     *    - convertPage() - 转换分页结果类型
     *    - createPageResult() - 创建分页结果
     * 
     * 4. 计算工具：
     *    - calculateOffset() - 计算偏移量
     *    - calculateTotalPages() - 计算总页数
     *    - getPageRange() - 获取分页范围
     * 
     * 使用示例：
     * 
     * // 创建分页请求
     * Pageable pageable = PageUtils.createPageable(1, 10);
     * 
     * // 带排序的分页请求
     * Sort sort = PageUtils.createSortDesc("createTime");
     * Pageable pageableWithSort = PageUtils.createPageable(1, 10, sort);
     * 
     * // 解析排序字符串
     * Sort parsedSort = PageUtils.parseSort("name:asc,createTime:desc");
     * 
     * // 转换分页结果
     * Page<User> userPage = userRepository.findAll(pageable);
     * PageResult<UserVO> userVOPage = PageUtils.convertPage(userPage, user -> {
     *     UserVO vo = new UserVO();
     *     BeanUtils.copyProperties(user, vo);
     *     return vo;
     * });
     * 
     * // 创建自定义分页结果
     * List<String> content = Arrays.asList("item1", "item2", "item3");
     * PageResult<String> result = PageUtils.createPageResult(content, 1, 10, 100);
     * 
     * // 获取分页范围信息
     * PageRange range = PageUtils.getPageRange(2, 10, 25);
     * System.out.println(range.getDescription()); // 输出：第 11-20 条，共 25 条记录
     * 
     * // 在Controller中使用
     * @GetMapping("/users")
     * public ResponseEntity<PageResult<UserVO>> getUsers(
     *         @RequestParam(defaultValue = "1") Integer page,
     *         @RequestParam(defaultValue = "10") Integer size,
     *         @RequestParam(required = false) String sort) {
     *     
     *     Sort sortObj = PageUtils.parseSort(sort);
     *     Pageable pageable = PageUtils.createPageable(page, size, sortObj);
     *     
     *     Page<User> userPage = userService.findAll(pageable);
     *     PageResult<UserVO> result = PageUtils.convertPage(userPage, this::convertToVO);
     *     
     *     return ResponseEntity.ok(result);
     * }
     * 
     * 配置说明：
     * 1. 默认页码：1（前端传入的页码）
     * 2. 默认每页大小：10
     * 3. 最大每页大小：100（防止一次查询过多数据）
     * 4. 最小每页大小：1
     * 
     * 注意事项：
     * 1. Spring Data JPA的页码从0开始，工具类会自动转换
     * 2. 排序字段名应与实体类属性名一致
     * 3. 大数据量分页查询可能影响性能，建议使用索引
     * 4. 分页参数应进行校验，防止恶意请求
     * 5. 排序字段应进行白名单校验，防止SQL注入
     * 6. 深分页（大页码）性能较差，建议使用游标分页
     */

}