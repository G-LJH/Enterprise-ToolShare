package com.toolshare.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;
    private final RequestUserInterceptor requestUserInterceptor;
    private final AuthenticationRequirementInterceptor authenticationRequirementInterceptor;
    private final RoleAuthorizationInterceptor roleAuthorizationInterceptor;
    private final OperationLogInterceptor operationLogInterceptor;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String corsAllowedOrigins;

    public WebMvcConfig(AuthenticationInterceptor authenticationInterceptor,
                        RequestUserInterceptor requestUserInterceptor,
                        AuthenticationRequirementInterceptor authenticationRequirementInterceptor,
                        RoleAuthorizationInterceptor roleAuthorizationInterceptor,
                        OperationLogInterceptor operationLogInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
        this.requestUserInterceptor = requestUserInterceptor;
        this.authenticationRequirementInterceptor = authenticationRequirementInterceptor;
        this.roleAuthorizationInterceptor = roleAuthorizationInterceptor;
        this.operationLogInterceptor = operationLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor).addPathPatterns("/api/**").order(0);
        registry.addInterceptor(requestUserInterceptor).addPathPatterns("/api/**").order(1);
        registry.addInterceptor(authenticationRequirementInterceptor).addPathPatterns("/api/**").order(2);
        registry.addInterceptor(roleAuthorizationInterceptor).addPathPatterns("/api/**").order(3);
        registry.addInterceptor(operationLogInterceptor).addPathPatterns("/api/**").order(4);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsAllowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
