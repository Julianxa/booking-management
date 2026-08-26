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

    private Frontend frontend = new Frontend();

    private CheckIn checkin = new CheckIn();

    private Octo octo = new Octo();

    @Getter
    @Setter
    public static class Frontend {
        private String baseUrl;
    }

    @Getter
    @Setter
    public static class CheckIn {
        private String path;
    }

    @Getter
    @Setter
    public static class Octo {
        private String apiKey = "";
        private String supplierId = "tramway";
        private String supplierName = "Hong Kong Tramways";
        private String currency = "HKD";
        private String timeZone = "Asia/Hong_Kong";
        private String defaultOptionId = "DEFAULT";
        private long holdTimeoutMinutes = 10;
        private String contactName = "";
        private String contactEmail = "";
        private String contactTelephone = "";
        private String contactWebsite = "";
        private String contactAddress = "";
    }
}