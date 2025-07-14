package com.baro13.readfast.controller.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebCorsConfig  implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(corsProperties.getAllowedOrigins().toArray(new String[0]))
            .allowedMethods(corsProperties.getAllowedMethods().toArray(new String[0]))
            .allowedHeaders(corsProperties.getAllowedHeaders().toArray(new String[0]))
            .exposedHeaders(corsProperties.getExposedHeaders().toArray(new String[0]))
            .allowCredentials(corsProperties.getAllowCredentials())
            .maxAge(corsProperties.getMaxAge());
    }
}
