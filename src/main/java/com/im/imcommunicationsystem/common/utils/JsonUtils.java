package com.im.imcommunicationsystem.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;


import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 * 提供JSON序列化和反序列化功能
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
public class JsonUtils {

    /**
     * ObjectMapper实例（线程安全）
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // 配置ObjectMapper
        configureObjectMapper();
    }

    /**
     * 配置ObjectMapper
     */
    private static void configureObjectMapper() {
        // 注册Java 8时间模块
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        
        // 禁用将日期写为时间戳
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 忽略未知属性
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // 忽略空值属性
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        // 允许单引号
        OBJECT_MAPPER.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        
        // 允许不带引号的字段名
        OBJECT_MAPPER.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    /**
     * 对象转JSON字符串
     * 
     * @param object 对象
     * @return JSON字符串
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON失败: {}", object.getClass().getSimpleName(), e);
            return null;
        }
    }

    /**
     * 对象转格式化的JSON字符串（美化输出）
     * 
     * @param object 对象
     * @return 格式化的JSON字符串
     */
    public static String toPrettyJson(Object object) {
        if (object == null) {
            return null;
        }
        
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("对象转格式化JSON失败: {}", object.getClass().getSimpleName(), e);
            return null;
        }
    }

    /**
     * JSON字符串转对象
     * 
     * @param json JSON字符串
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 对象实例
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json) || clazz == null) {
            return null;
        }
        
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON转对象失败: json={}, class={}", json, clazz.getSimpleName(), e);
            return null;
        }
    }

    /**
     * JSON字符串转对象（使用TypeReference）
     * 
     * @param json JSON字符串
     * @param typeReference 类型引用
     * @param <T> 泛型类型
     * @return 对象实例
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (!StringUtils.hasText(json) || typeReference == null) {
            return null;
        }
        
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("JSON转对象失败: json={}, typeReference={}", json, typeReference.getType(), e);
            return null;
        }
    }

    /**
     * JSON字符串转List
     * 
     * @param json JSON字符串
     * @param elementClass 元素类型
     * @param <T> 泛型类型
     * @return List对象
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> elementClass) {
        if (!StringUtils.hasText(json) || elementClass == null) {
            return null;
        }
        
        try {
            return OBJECT_MAPPER.readValue(json, 
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, elementClass));
        } catch (JsonProcessingException e) {
            log.error("JSON转List失败: json={}, elementClass={}", json, elementClass.getSimpleName(), e);
            return null;
        }
    }

    /**
     * JSON字符串转Map
     * 
     * @param json JSON字符串
     * @return Map对象
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON转Map失败: json={}", json, e);
            return null;
        }
    }

    /**
     * JSON字符串转Map（指定值类型）
     * 
     * @param json JSON字符串
     * @param valueClass 值类型
     * @param <T> 值的泛型类型
     * @return Map对象
     */
    public static <T> Map<String, T> fromJsonToMap(String json, Class<T> valueClass) {
        if (!StringUtils.hasText(json) || valueClass == null) {
            return null;
        }
        
        try {
            return OBJECT_MAPPER.readValue(json, 
                    OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, String.class, valueClass));
        } catch (JsonProcessingException e) {
            log.error("JSON转Map失败: json={}, valueClass={}", json, valueClass.getSimpleName(), e);
            return null;
        }
    }

    /**
     * 对象转JsonNode
     * 
     * @param object 对象
     * @return JsonNode
     */
    public static JsonNode toJsonNode(Object object) {
        if (object == null) {
            return null;
        }
        
        try {
            return OBJECT_MAPPER.valueToTree(object);
        } catch (Exception e) {
            log.error("对象转JsonNode失败: {}", object.getClass().getSimpleName(), e);
            return null;
        }
    }

    /**
     * JSON字符串转JsonNode
     * 
     * @param json JSON字符串
     * @return JsonNode
     */
    public static JsonNode fromJsonToNode(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("JSON转JsonNode失败: json={}", json, e);
            return null;
        }
    }

    /**
     * JsonNode转对象
     * 
     * @param jsonNode JsonNode
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 对象实例
     */
    public static <T> T fromJsonNode(JsonNode jsonNode, Class<T> clazz) {
        if (jsonNode == null || clazz == null) {
            return null;
        }
        
        try {
            return OBJECT_MAPPER.treeToValue(jsonNode, clazz);
        } catch (JsonProcessingException e) {
            log.error("JsonNode转对象失败: class={}", clazz.getSimpleName(), e);
            return null;
        }
    }

    /**
     * 对象深拷贝（通过JSON序列化实现）
     * 
     * @param object 源对象
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 拷贝后的对象
     */
    public static <T> T deepCopy(Object object, Class<T> clazz) {
        if (object == null || clazz == null) {
            return null;
        }
        
        try {
            String json = OBJECT_MAPPER.writeValueAsString(object);
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("对象深拷贝失败: sourceClass={}, targetClass={}", 
                    object.getClass().getSimpleName(), clazz.getSimpleName(), e);
            return null;
        }
    }

    /**
     * 合并两个JSON对象
     * 
     * @param json1 第一个JSON字符串
     * @param json2 第二个JSON字符串
     * @return 合并后的JSON字符串
     */
    public static String mergeJson(String json1, String json2) {
        if (!StringUtils.hasText(json1)) {
            return json2;
        }
        if (!StringUtils.hasText(json2)) {
            return json1;
        }
        
        try {
            JsonNode node1 = OBJECT_MAPPER.readTree(json1);
            JsonNode node2 = OBJECT_MAPPER.readTree(json2);
            
            JsonNode merged = mergeJsonNodes(node1, node2);
            return OBJECT_MAPPER.writeValueAsString(merged);
        } catch (Exception e) {
            log.error("JSON合并失败: json1={}, json2={}", json1, json2, e);
            return json1;
        }
    }

    /**
     * 合并两个JsonNode
     * 
     * @param node1 第一个节点
     * @param node2 第二个节点
     * @return 合并后的节点
     */
    private static JsonNode mergeJsonNodes(JsonNode node1, JsonNode node2) {
        if (node1.isObject() && node2.isObject()) {
            com.fasterxml.jackson.databind.node.ObjectNode result = OBJECT_MAPPER.createObjectNode();
            result.setAll((com.fasterxml.jackson.databind.node.ObjectNode) node1);
            result.setAll((com.fasterxml.jackson.databind.node.ObjectNode) node2);
            return result;
        }
        return node2; // 如果不是对象类型，用第二个节点覆盖第一个
    }

    /**
     * 验证JSON字符串格式
     * 
     * @param json JSON字符串
     * @return 是否为有效的JSON
     */
    public static boolean isValidJson(String json) {
        if (!StringUtils.hasText(json)) {
            return false;
        }
        
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 从JSON中提取指定路径的值
     * 
     * @param json JSON字符串
     * @param path JSON路径（如："user.name"）
     * @return 值的字符串表示
     */
    public static String extractValue(String json, String path) {
        if (!StringUtils.hasText(json) || !StringUtils.hasText(path)) {
            return null;
        }
        
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(json);
            JsonNode valueNode = rootNode.at("/" + path.replace(".", "/"));
            
            if (valueNode.isMissingNode()) {
                return null;
            }
            
            return valueNode.isTextual() ? valueNode.asText() : valueNode.toString();
        } catch (Exception e) {
            log.error("提取JSON值失败: json={}, path={}", json, path, e);
            return null;
        }
    }

    /**
     * 从JSON中提取指定路径的值并转换为指定类型
     * 
     * @param json JSON字符串
     * @param path JSON路径
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 转换后的值
     */
    public static <T> T extractValue(String json, String path, Class<T> clazz) {
        String valueStr = extractValue(json, path);
        if (valueStr == null) {
            return null;
        }
        
        return fromJson(valueStr, clazz);
    }

    /**
     * 移除JSON中的敏感字段
     * 
     * @param json JSON字符串
     * @param sensitiveFields 敏感字段列表
     * @return 移除敏感字段后的JSON
     */
    public static String removeSensitiveFields(String json, String... sensitiveFields) {
        if (!StringUtils.hasText(json) || sensitiveFields == null || sensitiveFields.length == 0) {
            return json;
        }
        
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(json);
            if (rootNode.isObject()) {
                com.fasterxml.jackson.databind.node.ObjectNode objectNode = 
                        (com.fasterxml.jackson.databind.node.ObjectNode) rootNode;
                
                for (String field : sensitiveFields) {
                    objectNode.remove(field);
                }
                
                return OBJECT_MAPPER.writeValueAsString(objectNode);
            }
            return json;
        } catch (Exception e) {
            log.error("移除敏感字段失败: json={}", json, e);
            return json;
        }
    }

    /**
     * 压缩JSON（移除空格和换行）
     * 
     * @param json JSON字符串
     * @return 压缩后的JSON
     */
    public static String compactJson(String json) {
        if (!StringUtils.hasText(json)) {
            return json;
        }
        
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            return OBJECT_MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            log.error("JSON压缩失败: json={}", json, e);
            return json;
        }
    }

    /**
     * 获取ObjectMapper实例
     * 
     * @return ObjectMapper实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * JSON工具使用说明：
     * 
     * 1. 基础序列化：
     *    - toJson() - 对象转JSON字符串
     *    - toPrettyJson() - 对象转格式化JSON字符串
     *    - fromJson() - JSON字符串转对象
     * 
     * 2. 集合转换：
     *    - fromJsonToList() - JSON转List
     *    - fromJsonToMap() - JSON转Map
     * 
     * 3. JsonNode操作：
     *    - toJsonNode() - 对象转JsonNode
     *    - fromJsonToNode() - JSON字符串转JsonNode
     *    - fromJsonNode() - JsonNode转对象
     * 
     * 4. 高级功能：
     *    - deepCopy() - 对象深拷贝
     *    - mergeJson() - JSON合并
     *    - isValidJson() - JSON格式验证
     *    - extractValue() - 提取JSON值
     *    - removeSensitiveFields() - 移除敏感字段
     *    - compactJson() - JSON压缩
     * 
     * 使用示例：
     * 
     * // 对象序列化
     * User user = new User("张三", 25);
     * String json = JsonUtils.toJson(user);
     * 
     * // JSON反序列化
     * User user2 = JsonUtils.fromJson(json, User.class);
     * 
     * // List转换
     * List<User> users = JsonUtils.fromJsonToList(jsonArray, User.class);
     * 
     * // Map转换
     * Map<String, Object> map = JsonUtils.fromJsonToMap(json);
     * 
     * // 深拷贝
     * User userCopy = JsonUtils.deepCopy(user, User.class);
     * 
     * // 提取值
     * String name = JsonUtils.extractValue(json, "user.name");
     * 
     * // 移除敏感字段
     * String safeJson = JsonUtils.removeSensitiveFields(json, "password", "token");
     * 
     * // 验证JSON格式
     * if (JsonUtils.isValidJson(jsonString)) {
     *     // 处理有效的JSON
     * }
     * 
     * 配置说明：
     * 1. 忽略未知属性：反序列化时忽略JSON中多余的字段
     * 2. 时间格式：使用ISO-8601格式而不是时间戳
     * 3. 空值处理：序列化时不会因为空对象而失败
     * 4. 引号支持：支持单引号和不带引号的字段名
     * 
     * 注意事项：
     * 1. ObjectMapper是线程安全的，可以在多线程环境中使用
     * 2. 序列化失败时返回null，建议检查返回值
     * 3. 敏感信息应在序列化前移除或使用@JsonIgnore注解
     * 4. 大对象序列化可能消耗较多内存，注意性能影响
     * 5. 循环引用会导致序列化失败，需要使用@JsonIgnore或@JsonManagedReference/@JsonBackReference
     */

}