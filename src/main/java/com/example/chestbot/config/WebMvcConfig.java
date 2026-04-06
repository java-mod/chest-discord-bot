package com.example.chestbot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final DashboardAuthInterceptor dashboardAuthInterceptor;
    private final IslandDashboardInterceptor islandDashboardInterceptor;

    public WebMvcConfig(DashboardAuthInterceptor dashboardAuthInterceptor,
                        IslandDashboardInterceptor islandDashboardInterceptor) {
        this.dashboardAuthInterceptor = dashboardAuthInterceptor;
        this.islandDashboardInterceptor = islandDashboardInterceptor;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/dashboard");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 관리자 전용: /dashboard/** 에서 /dashboard/island/** 제외
        registry.addInterceptor(dashboardAuthInterceptor)
                .addPathPatterns("/dashboard/**")
                .excludePathPatterns("/dashboard/island/**");

        // 섬장 전용: /dashboard/island/**
        registry.addInterceptor(islandDashboardInterceptor)
                .addPathPatterns("/dashboard/island/**");
    }
}
