package com.example.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.model.dto.ErrorResponseDTO;
import com.example.service.AwsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class CognitoTokenFilter extends OncePerRequestFilter {
    private final AwsService awsService;
    private final List<String> filterPaths;

    public CognitoTokenFilter(AwsService awsService, List<String> filterPaths) {
        this.awsService = awsService;
        this.filterPaths = filterPaths;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return filterPaths.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader("X-Access-Token");
        if (authorizationHeader == null) {
            ErrorResponseDTO error = ErrorResponseDTO.builder()
                    .message("Missing X-Access-Token header")
                    .timestamp(LocalDateTime.now().toString())
                    .build();
            response.setStatus(HttpStatus.UNAUTHORIZED.value()); // 401
            response.setContentType("application/json");
            response.getWriter().write(new ObjectMapper().writeValueAsString(error));
            return;
        }

        String accessToken = authorizationHeader.replace("Bearer ", "");
        if (!awsService.isAccessTokenValid(accessToken)) {
            ErrorResponseDTO error = ErrorResponseDTO.builder()
                    .message("Access token is revoked or invalid")
                    .timestamp(LocalDateTime.now().toString())
                    .build();
            response.setStatus(HttpStatus.UNAUTHORIZED.value()); // 401
            response.setContentType("application/json");
            response.getWriter().write(new ObjectMapper().writeValueAsString(error));
            return;
        }
        filterChain.doFilter(request, response);
    }
}