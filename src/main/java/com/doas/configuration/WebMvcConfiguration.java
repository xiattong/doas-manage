package com.doas.configuration;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    @Resource
    private ApplicationContext applicationContext;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("*").allowedMethods("*")
                .allowedHeaders("*").allowCredentials(true).exposedHeaders(HttpHeaders.SET_COOKIE).maxAge(3600L);
    }
}
