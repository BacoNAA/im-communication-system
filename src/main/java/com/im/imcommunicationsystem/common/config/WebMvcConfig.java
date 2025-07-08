package com.im.imcommunicationsystem.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Web MVC配置类
 * 配置静态资源处理，避免与API路径冲突
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置静态资源处理
     * 明确指定静态资源路径，避免与API路径冲突
     * 
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源映射
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        
        registry.addResourceHandler("/*.html")
                .addResourceLocations("classpath:/static/");
        
        // 确保API路径不被静态资源处理器处理
        // Spring Boot默认会将所有未匹配的路径当作静态资源处理
        // 通过明确配置静态资源路径，可以避免API路径被误处理
    }
    
    /**
     * 配置路径匹配策略
     * 确保API路径正确匹配到Controller而不是静态资源处理器
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 使用PathPatternParser来提高路径匹配性能和准确性
        configurer.setPatternParser(new PathPatternParser());
        // 确保尾部斜杠匹配
        configurer.setUseTrailingSlashMatch(true);
    }
}