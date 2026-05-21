package com.example.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.config.FilterPathConfig;
import com.example.exception.user.InvalidAccessTokenException;
import com.example.service.AwsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CognitoTokenFilter extends OncePerRequestFilter {
    private final AwsService awsService;
    private final List<String> filterPaths;

    public CognitoTokenFilter(AwsService awsService, FilterPathConfig filterPathConfig) {
        this.awsService = awsService;
        this.filterPaths = filterPathConfig.getPaths();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return filterPaths.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) {
        String authorizationHeader = request.getHeader("X-Access-Token");

        if (authorizationHeader == null) {
            throw new InvalidAccessTokenException("Missing X-Access-Token header");
        }

        String accessToken = authorizationHeader.replace("Bearer ", "").trim();

        if (!awsService.isAccessTokenValid(accessToken)) {
            throw new InvalidAccessTokenException("Access token is revoked or invalid");
        }

        try {
            DecodedJWT decodedJWT = JWT.decode(accessToken);
            String userSub = decodedJWT.getSubject();

            List<String> groups = decodedJWT.getClaim("cognito:groups").asList(String.class);

            List<GrantedAuthority> authorities;

            if (groups == null || groups.isEmpty()) {
                authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            } else {
                authorities = groups.stream()
                        .map(group -> new SimpleGrantedAuthority("ROLE_" + group.toUpperCase()))
                        .collect(Collectors.toList());
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userSub, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (ServletException | IOException e) {
            SecurityContextHolder.clearContext();
            throw new InvalidAccessTokenException("Invalid or expired token");
        }
    }
}