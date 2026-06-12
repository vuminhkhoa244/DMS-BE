package com.example.DocumentManagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private int presignedExpiryMinutes = 60;
    private String publicEndpoint;
    /** Required when pointing at AWS S3 (e.g. "ap-southeast-1"). Optional for MinIO. */
    private String region;
}
