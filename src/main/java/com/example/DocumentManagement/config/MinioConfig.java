package com.example.DocumentManagement.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    private final MinioProperties props;
    private final MinioClient cachedClient;

    public MinioConfig(MinioProperties props) {
        this.props = props;
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey());
        if (props.getRegion() != null && !props.getRegion().isBlank()) {
            builder.region(props.getRegion());
        }
        this.cachedClient = builder.build();
    }

    @Bean
    public MinioClient minioClient() {
        return cachedClient;
    }

    /**
     * Runs after the Spring context is fully initialized so we avoid any
     * chicken-and-egg with the MinioClient bean creation.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ensureBucket() {
        try {
            boolean exists = cachedClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(props.getBucket())
                    .build());
            if (!exists) {
                cachedClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(props.getBucket())
                        .build());
                log.info("Created MinIO bucket: {}", props.getBucket());
            } else {
                log.info("MinIO bucket ready: {}", props.getBucket());
            }
        } catch (Exception e) {
            log.error("Failed to ensure MinIO bucket '{}': {}", props.getBucket(), e.getMessage());
        }
    }
}
