package com.example.DocumentManagement.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.DocumentManagement.config.CloudinaryProperties;
import com.example.DocumentManagement.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    private final Cloudinary cloudinary;
    private final CloudinaryProperties props;

    public CloudinaryService(Cloudinary cloudinary, CloudinaryProperties props) {
        this.cloudinary = cloudinary;
        this.props = props;
    }

    public String upload(MultipartFile file, String objectKey) {
        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "public_id", objectKey.replaceAll("/", "_"),
                    "resource_type", "auto",
                    "folder", props.getFolder(),
                    "overwrite", true
            );
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), params);
            String publicId = (String) result.get("public_id");
            return publicId;
        } catch (IOException e) {
            throw new BadRequestException("Failed to read upload: " + e.getMessage());
        } catch (Exception e) {
            log.error("Cloudinary upload failed for key {}", objectKey, e);
            throw new BadRequestException("Upload failed: " + e.getMessage());
        }
    }

    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.warn("Failed to delete Cloudinary object {}: {}", publicId, e.getMessage());
        }
    }

    public String getDownloadUrl(String publicId) {
        try {
            return cloudinary.url()
                    .publicId(publicId)
                    .secure(true)
                    .generate();
        } catch (Exception e) {
            log.error("Failed to generate Cloudinary URL for {}", publicId, e);
            throw new BadRequestException("Could not generate download URL");
        }
    }

    public String buildKey(String namespace, String id, String originalFilename) {
        String safe = sanitize(originalFilename);
        return "%s_%s_%s_%s".formatted(namespace, id, java.util.UUID.randomUUID(), safe);
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) return "file";
        String name = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return name.length() > 120 ? name.substring(name.length() - 120) : name;
    }
}
