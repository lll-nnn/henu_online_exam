package com.lee.interceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpringMvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns
//                ("/", "/admin_login","/**/**/*.js", "/**/**/*.css", "/**/**/*.png","/teacher/login", "/student_login");
        registry.addInterceptor(new AdminInterceptor()).addPathPatterns("/admin/**").addPathPatterns("/admin");
        registry.addInterceptor(new TeacherInterceptor()).addPathPatterns("/teacher/**").addPathPatterns("/teacher");
        registry.addInterceptor(new StudentInterceptor()).addPathPatterns("/student/**").addPathPatterns("/student");
    }
}
