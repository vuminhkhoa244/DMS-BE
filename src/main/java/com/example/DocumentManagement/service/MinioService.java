package com.example.DocumentManagement.service;

import com.example.DocumentManagement.config.MinioProperties;
import com.example.DocumentManagement.exception.BadRequestException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    private final MinioClient minioClient;
    private final MinioProperties props;

    public MinioService(MinioClient minioClient, MinioProperties props) {
        this.minioClient = minioClient;
        this.props = props;
    }

    /**
     * Upload a MultipartFile to MinIO. Returns the object key that was stored.
     * The bucket name is NEVER persisted — callers store only the returned key.
     */
    public String upload(MultipartFile file, String objectKey) {
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .build());
            return objectKey;
        } catch (IOException e) {
            throw new BadRequestException("Failed to read upload: " + e.getMessage());
        } catch (Exception e) {
            log.error("MinIO upload failed for key {}", objectKey, e);
            throw new BadRequestException("Upload failed: " + e.getMessage());
        }
    }

    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete MinIO object {}: {}", objectKey, e.getMessage());
        }
    }

    /**
     * Generate a time-limited presigned GET URL. The URL is derived from config every time
     * and is never persisted — if you migrate to S3, change env and you're done.
     */
    public String presignedGetUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .expiry(props.getPresignedExpiryMinutes(), TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("Failed to presign MinIO object {}", objectKey, e);
            throw new BadRequestException("Could not generate download URL");
        }
    }

    /**
     * Build an object key under a stable namespace.
     * Personal files:  personal/{userId}/{uuid}-{originalName}
     * Org files:       orgs/{orgId}/{uuid}-{originalName}
     */
    public String buildKey(String namespace, String id, String originalFilename) {
        String safe = sanitize(originalFilename);
        return "%s/%s/%s-%s".formatted(namespace, id, UUID.randomUUID(), safe);
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) return "file";
        String name = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return name.length() > 120 ? name.substring(name.length() - 120) : name;
    }
}
