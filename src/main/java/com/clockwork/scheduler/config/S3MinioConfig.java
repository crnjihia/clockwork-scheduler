package com.clockwork.scheduler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class S3MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(S3MinioConfig.class);

    @Value("${app.s3.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${app.s3.access-key:minioadmin}")
    private String accessKey;

    @Value("${app.s3.secret-key:minioadmin}")
    private String secretKey;

    @Value("${app.s3.region:us-east-1}")
    private String region;

    @Bean
    public S3Client s3Client() {
        try {
            log.info("Configuring S3/MinIO client pointing to endpoint: {}", endpoint);
            return S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)
                    ))
                    .region(Region.of(region))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build())
                    .build();
        } catch (Exception e) {
            log.warn("Could not initialize S3Client with endpoint {}: {}. Fallback storage will be used.", endpoint, e.getMessage());
            return null;
        }
    }
}
