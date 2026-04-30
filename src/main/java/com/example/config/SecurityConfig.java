package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "https://d17c6q888ubvgt.cloudfront.net",
                "https://db6to9tswfrc9.cloudfront.net",
                "https://tramway.duckdns.org",
                "https://tramway.duckdns.org:8080",
                "http://localhost:3000",
                "http://localhost:3001"
        ));

        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                // Put CORS filter very early
                .addFilterBefore(corsFilter(), org.springframework.security.web.header.HeaderWriterFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}