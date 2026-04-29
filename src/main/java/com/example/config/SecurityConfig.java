package com.example.config;


import com.example.service.AwsService;
import com.example.utils.CognitoTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    @Value("${filter.paths}")
    private List<String> filterPaths;
    
    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private String allowedOriginsConfig;
    
    @Bean
    public CognitoTokenFilter cognitoTokenFilter(AwsService awsService) {
        return new CognitoTokenFilter(awsService, filterPaths());
    }
    @Bean
    public List<String> filterPaths() {
        return filterPaths;
    }

    private final AwsService awsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Parse CORS allowed origins from config
        List<String> allowedOrigins = Arrays.asList(allowedOriginsConfig.split(","));
        
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless REST API
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(allowedOrigins);
                    config.setAllowedMethods(List.of("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setExposedHeaders(List.of("Content-Type", "Authorization"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L); // Cache preflight responses for 1 hour
                    return config;
                }))
                .authorizeHttpRequests(auth -> auth
                        // Permit unauthenticated access to Swagger UI and API docs
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/**", "/api/v1/tokens/**", "/api/v1/organizations/**", "/api/v1/events/**", "/api/v1/bookings/**", "/api/v1/gift-certificates/**", "/api/v1/emails/**").permitAll()
//                        // Allow registration and login without authentication
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/**", "/api/v1/tokens/**", "/api/v1/organizations/**", "/api/v1/events/**", "/api/v1/bookings/**", "/api/v1/gift-certificates/**", "/api/v1/emails/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/**", "/api/v1/tokens/**", "/api/v1/organizations/**", "/api/v1/events/**", "/api/v1/bookings/**", "/api/v1/gift-certificates/**", "/api/v1/emails/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**", "/api/v1/tokens/**", "/api/v1/organizations/**", "/api/v1/events/**", "/api/v1/bookings/**", "/api/v1/gift-certificates/**", "/api/v1/emails/**").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(cognitoTokenFilter(awsService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
