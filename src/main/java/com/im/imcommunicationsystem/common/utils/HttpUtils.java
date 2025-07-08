package com.im.imcommunicationsystem.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP工具类
 * 提供HTTP请求、响应处理相关功能
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
public class HttpUtils {

    /**
     * RestTemplate实例
     */
    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    /**
     * 常用Content-Type
     */
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_XML = "application/xml";
    public static final String CONTENT_TYPE_TEXT = "text/plain";
    public static final String CONTENT_TYPE_HTML = "text/html";
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";

    /**
     * 常用请求头
     */
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String HEADER_X_REAL_IP = "X-Real-IP";
    public static final String HEADER_REFERER = "Referer";

    /**
     * 获取当前HTTP请求
     * 
     * @return HttpServletRequest
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取当前HTTP响应
     * 
     * @return HttpServletResponse
     */
    public static HttpServletResponse getResponse() {
        ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getResponse() : null;
    }

    /**
     * 获取请求参数Map
     * 
     * @param request HTTP请求
     * @return 参数Map
     */
    public static Map<String, String> getParameterMap(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        if (request != null) {
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String paramValue = request.getParameter(paramName);
                params.put(paramName, paramValue);
            }
        }
        return params;
    }

    /**
     * 获取请求头Map
     * 
     * @param request HTTP请求
     * @return 请求头Map
     */
    public static Map<String, String> getHeaderMap(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        if (request != null) {
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                headers.put(headerName, headerValue);
            }
        }
        return headers;
    }

    /**
     * 获取请求的完整URL
     * 
     * @param request HTTP请求
     * @return 完整URL
     */
    public static String getFullUrl(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        StringBuffer url = request.getRequestURL();
        String queryString = request.getQueryString();
        
        if (StringUtils.hasText(queryString)) {
            url.append("?").append(queryString);
        }
        
        return url.toString();
    }

    /**
     * 获取请求体内容
     * 
     * @param request HTTP请求
     * @return 请求体字符串
     */
    public static String getRequestBody(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            log.error("读取请求体失败", e);
            return null;
        }
    }

    /**
     * 判断是否为Ajax请求
     * 
     * @param request HTTP请求
     * @return 是否为Ajax请求
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestedWith);
    }

    /**
     * 判断是否为JSON请求
     * 
     * @param request HTTP请求
     * @return 是否为JSON请求
     */
    public static boolean isJsonRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        
        String contentType = request.getContentType();
        return contentType != null && contentType.contains(CONTENT_TYPE_JSON);
    }

    /**
     * 判断是否为移动端请求
     * 
     * @param request HTTP请求
     * @return 是否为移动端请求
     */
    public static boolean isMobileRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        
        String userAgent = request.getHeader(HEADER_USER_AGENT);
        if (!StringUtils.hasText(userAgent)) {
            return false;
        }
        
        userAgent = userAgent.toLowerCase();
        return userAgent.contains("mobile") || 
               userAgent.contains("android") || 
               userAgent.contains("iphone") || 
               userAgent.contains("ipad") || 
               userAgent.contains("windows phone");
    }

    /**
     * 设置响应为JSON格式
     * 
     * @param response HTTP响应
     */
    public static void setJsonResponse(HttpServletResponse response) {
        if (response != null) {
            response.setContentType(CONTENT_TYPE_JSON);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }
    }

    /**
     * 设置响应头
     * 
     * @param response HTTP响应
     * @param name 头名称
     * @param value 头值
     */
    public static void setResponseHeader(HttpServletResponse response, String name, String value) {
        if (response != null && StringUtils.hasText(name)) {
            response.setHeader(name, value);
        }
    }

    /**
     * 写入响应内容
     * 
     * @param response HTTP响应
     * @param content 响应内容
     */
    public static void writeResponse(HttpServletResponse response, String content) {
        if (response == null || !StringUtils.hasText(content)) {
            return;
        }
        
        try (PrintWriter writer = response.getWriter()) {
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            log.error("写入响应失败", e);
        }
    }

    /**
     * 写入JSON响应
     * 
     * @param response HTTP响应
     * @param object 响应对象
     */
    public static void writeJsonResponse(HttpServletResponse response, Object object) {
        if (response == null || object == null) {
            return;
        }
        
        setJsonResponse(response);
        String json = JsonUtils.toJson(object);
        writeResponse(response, json);
    }

    /**
     * URL编码
     * 
     * @param str 待编码字符串
     * @return 编码后的字符串
     */
    public static String urlEncode(String str) {
        if (!StringUtils.hasText(str)) {
            return str;
        }
        
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.error("URL编码失败: {}", str, e);
            return str;
        }
    }

    /**
     * URL解码
     * 
     * @param str 待解码字符串
     * @return 解码后的字符串
     */
    public static String urlDecode(String str) {
        if (!StringUtils.hasText(str)) {
            return str;
        }
        
        try {
            return URLDecoder.decode(str, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.error("URL解码失败: {}", str, e);
            return str;
        }
    }

    /**
     * 发送GET请求
     * 
     * @param url 请求URL
     * @return 响应字符串
     */
    public static String sendGet(String url) {
        return sendGet(url, null);
    }

    /**
     * 发送GET请求
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @return 响应字符串
     */
    public static String sendGet(String url, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            ResponseEntity<String> response = REST_TEMPLATE.exchange(
                    url, HttpMethod.GET, entity, String.class);
            
            return response.getBody();
        } catch (Exception e) {
            log.error("GET请求失败: url={}", url, e);
            return null;
        }
    }

    /**
     * 发送POST请求（JSON格式）
     * 
     * @param url 请求URL
     * @param requestBody 请求体
     * @return 响应字符串
     */
    public static String sendPostJson(String url, Object requestBody) {
        return sendPostJson(url, requestBody, null);
    }

    /**
     * 发送POST请求（JSON格式）
     * 
     * @param url 请求URL
     * @param requestBody 请求体
     * @param headers 请求头
     * @return 响应字符串
     */
    public static String sendPostJson(String url, Object requestBody, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            String jsonBody = JsonUtils.toJson(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, httpHeaders);
            
            ResponseEntity<String> response = REST_TEMPLATE.exchange(
                    url, HttpMethod.POST, entity, String.class);
            
            return response.getBody();
        } catch (Exception e) {
            log.error("POST JSON请求失败: url={}", url, e);
            return null;
        }
    }

    /**
     * 发送POST请求（表单格式）
     * 
     * @param url 请求URL
     * @param params 表单参数
     * @return 响应字符串
     */
    public static String sendPostForm(String url, Map<String, String> params) {
        return sendPostForm(url, params, null);
    }

    /**
     * 发送POST请求（表单格式）
     * 
     * @param url 请求URL
     * @param params 表单参数
     * @param headers 请求头
     * @return 响应字符串
     */
    public static String sendPostForm(String url, Map<String, String> params, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            StringBuilder formData = new StringBuilder();
            if (params != null) {
                params.forEach((key, value) -> {
                    if (formData.length() > 0) {
                        formData.append("&");
                    }
                    formData.append(urlEncode(key)).append("=").append(urlEncode(value));
                });
            }
            
            HttpEntity<String> entity = new HttpEntity<>(formData.toString(), httpHeaders);
            
            ResponseEntity<String> response = REST_TEMPLATE.exchange(
                    url, HttpMethod.POST, entity, String.class);
            
            return response.getBody();
        } catch (Exception e) {
            log.error("POST Form请求失败: url={}", url, e);
            return null;
        }
    }

    /**
     * 发送PUT请求
     * 
     * @param url 请求URL
     * @param requestBody 请求体
     * @return 响应字符串
     */
    public static String sendPut(String url, Object requestBody) {
        return sendPut(url, requestBody, null);
    }

    /**
     * 发送PUT请求
     * 
     * @param url 请求URL
     * @param requestBody 请求体
     * @param headers 请求头
     * @return 响应字符串
     */
    public static String sendPut(String url, Object requestBody, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            String jsonBody = JsonUtils.toJson(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, httpHeaders);
            
            ResponseEntity<String> response = REST_TEMPLATE.exchange(
                    url, HttpMethod.PUT, entity, String.class);
            
            return response.getBody();
        } catch (Exception e) {
            log.error("PUT请求失败: url={}", url, e);
            return null;
        }
    }

    /**
     * 发送DELETE请求
     * 
     * @param url 请求URL
     * @return 响应字符串
     */
    public static String sendDelete(String url) {
        return sendDelete(url, null);
    }

    /**
     * 发送DELETE请求
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @return 响应字符串
     */
    public static String sendDelete(String url, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            ResponseEntity<String> response = REST_TEMPLATE.exchange(
                    url, HttpMethod.DELETE, entity, String.class);
            
            return response.getBody();
        } catch (Exception e) {
            log.error("DELETE请求失败: url={}", url, e);
            return null;
        }
    }

    /**
     * 构建查询参数字符串
     * 
     * @param params 参数Map
     * @return 查询参数字符串
     */
    public static String buildQueryString(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        
        StringBuilder queryString = new StringBuilder();
        params.forEach((key, value) -> {
            if (value != null) {
                if (queryString.length() > 0) {
                    queryString.append("&");
                }
                queryString.append(urlEncode(key))
                          .append("=")
                          .append(urlEncode(value.toString()));
            }
        });
        
        return queryString.toString();
    }

    /**
     * 解析查询参数字符串
     * 
     * @param queryString 查询参数字符串
     * @return 参数Map
     */
    public static Map<String, String> parseQueryString(String queryString) {
        Map<String, String> params = new HashMap<>();
        
        if (!StringUtils.hasText(queryString)) {
            return params;
        }
        
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = urlDecode(keyValue[0]);
                String value = urlDecode(keyValue[1]);
                params.put(key, value);
            }
        }
        
        return params;
    }

    /**
     * 获取RestTemplate实例
     * 
     * @return RestTemplate实例
     */
    public static RestTemplate getRestTemplate() {
        return REST_TEMPLATE;
    }

    /**
     * HTTP工具使用说明：
     * 
     * 1. 请求信息获取：
     *    - getRequest() - 获取当前HTTP请求
     *    - getResponse() - 获取当前HTTP响应
     *    - getParameterMap() - 获取请求参数
     *    - getHeaderMap() - 获取请求头
     *    - getFullUrl() - 获取完整URL
     *    - getRequestBody() - 获取请求体
     * 
     * 2. 请求类型判断：
     *    - isAjaxRequest() - 判断是否为Ajax请求
     *    - isJsonRequest() - 判断是否为JSON请求
     *    - isMobileRequest() - 判断是否为移动端请求
     * 
     * 3. 响应处理：
     *    - setJsonResponse() - 设置JSON响应格式
     *    - setResponseHeader() - 设置响应头
     *    - writeResponse() - 写入响应内容
     *    - writeJsonResponse() - 写入JSON响应
     * 
     * 4. URL编码：
     *    - urlEncode() - URL编码
     *    - urlDecode() - URL解码
     * 
     * 5. HTTP请求发送：
     *    - sendGet() - 发送GET请求
     *    - sendPostJson() - 发送POST JSON请求
     *    - sendPostForm() - 发送POST表单请求
     *    - sendPut() - 发送PUT请求
     *    - sendDelete() - 发送DELETE请求
     * 
     * 6. 参数处理：
     *    - buildQueryString() - 构建查询参数
     *    - parseQueryString() - 解析查询参数
     * 
     * 使用示例：
     * 
     * // 获取当前请求信息
     * HttpServletRequest request = HttpUtils.getRequest();
     * Map<String, String> params = HttpUtils.getParameterMap(request);
     * 
     * // 判断请求类型
     * if (HttpUtils.isAjaxRequest(request)) {
     *     // 处理Ajax请求
     * }
     * 
     * // 设置JSON响应
     * HttpServletResponse response = HttpUtils.getResponse();
     * HttpUtils.writeJsonResponse(response, result);
     * 
     * // 发送HTTP请求
     * String result = HttpUtils.sendGet("https://api.example.com/data");
     * 
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Authorization", "Bearer token");
     * String jsonResult = HttpUtils.sendPostJson(url, requestData, headers);
     * 
     * // URL编码
     * String encoded = HttpUtils.urlEncode("中文参数");
     * 
     * // 构建查询参数
     * Map<String, Object> params = new HashMap<>();
     * params.put("name", "张三");
     * params.put("age", 25);
     * String queryString = HttpUtils.buildQueryString(params);
     * 
     * 注意事项：
     * 1. 在非Web环境中，getRequest()和getResponse()可能返回null
     * 2. HTTP请求发送失败时返回null，需要检查返回值
     * 3. 请求体读取是一次性的，重复读取会失败
     * 4. 敏感信息（如密码、token）不应记录在日志中
     * 5. 大文件上传下载建议使用专门的文件处理工具
     * 6. 长时间的HTTP请求可能需要设置超时时间
     */

}