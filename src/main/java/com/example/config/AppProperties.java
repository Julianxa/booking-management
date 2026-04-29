package com.example.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private String baseUrl;

    private CheckIn checkin = new CheckIn();

    @Getter
    @Setter
    public static class CheckIn {
        private String path;
    }
}