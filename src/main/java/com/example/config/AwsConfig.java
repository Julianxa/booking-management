package com.example.config;

import software.amazon.awssdk.services.kms.KmsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@Configuration
public class AwsConfig {
    @Value("${aws.region}")
    private String region;
    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;
    @Value("${aws.cognito.client-id}")
    private String clientId;
    @Value("${aws.secret-key}")
    private String secretKey;
    @Value("${aws.app-secret-key}")
    private String appSecretKey;
    @Value("${aws.access-key}")
    private String accessKey;
    @Value("${aws.kms-cmk-id}")
    private String cmkKeyId;
    @Value("${aws.bucket-name}")
    private String bucketName;

    @Bean
    public CognitoIdentityProviderClient cognitoClient() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Bean
    public KmsClient kmsClient() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return KmsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Bean
    public S3TransferManager s3TransferManager() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        S3AsyncClient s3AsyncClient = S3AsyncClient.crtBuilder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
//                .targetThroughputInGbps(20.0)
//                .minimumPartSizeInBytes(8 * MB)
                .build();

        return S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }


    @Bean
    public String region() {
        return region;
    }
    @Bean
    public String userPoolId() {
        return userPoolId;
    }
    @Bean
    public String clientId() {
        return clientId;
    }
    @Bean
    public String appSecretKey() {
        return appSecretKey;
    }
    @Bean
    public String accessKey() {
        return accessKey;
    }
    @Bean
    public String cmkKeyId() {
        return cmkKeyId;
    }
    @Bean
    public String bucketName() {
        return bucketName;
    }
}

