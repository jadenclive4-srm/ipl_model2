package com.ipl.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FrontendResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/frontend22/build/static/");
        
        registry.addResourceHandler("/logos/**")
                .addResourceLocations("classpath:/frontend22/build/logos/");
        
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/frontend22/build/favicon.ico");
        
        registry.addResourceHandler("/manifest.json")
                .addResourceLocations("classpath:/frontend22/build/manifest.json");
        
        registry.addResourceHandler("/asset-manifest.json")
                .addResourceLocations("classpath:/frontend22/build/asset-manifest.json");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/frontend22/build/index.html");
    }
}