package com.sc.ares.features.company.service;

import com.sc.ares.common.api.ApiBadRequestException;
import com.sc.ares.common.api.ResourceNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompanyLogoStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
        MediaType.IMAGE_PNG_VALUE, ".png",
        MediaType.IMAGE_JPEG_VALUE, ".jpg",
        "image/webp", ".webp",
        "image/svg+xml", ".svg"
    );

    private final Path storageDirectory;

    public CompanyLogoStorageService(
        @Value("${ares.storage.company-logo-dir:}") String configuredStorageDirectory
    ) {
        this.storageDirectory = configuredStorageDirectory == null || configuredStorageDirectory.isBlank()
            ? Path.of(System.getProperty("java.io.tmpdir"), "ares", "company-logos")
            : Path.of(configuredStorageDirectory);
    }

    public String storeLogo(UUID companyId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiBadRequestException("Company logo file is required.");
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.containsKey(contentType)) {
            throw new ApiBadRequestException("Company logo must be a PNG, JPG, WEBP, or SVG image.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ApiBadRequestException("Company logo must be 5 MB or smaller.");
        }

        try {
            Files.createDirectories(storageDirectory);

            String fileName = companyId + "-" + UUID.randomUUID() + ALLOWED_CONTENT_TYPES.get(contentType);
            Path destination = storageDirectory.resolve(fileName).normalize();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            return fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store company logo.", exception);
        }
    }

    public Resource getLogoResource(String fileName) {
        Path logoPath = storageDirectory.resolve(fileName).normalize();

        if (!logoPath.startsWith(storageDirectory) || !Files.exists(logoPath) || !Files.isRegularFile(logoPath)) {
            throw new ResourceNotFoundException("Company logo was not found.");
        }

        try {
            return new UrlResource(logoPath.toUri());
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Company logo was not found.");
        }
    }

    public MediaType getMediaType(String fileName) {
        Path logoPath = storageDirectory.resolve(fileName).normalize();

        try {
            String contentType = Files.probeContentType(logoPath);

            if (contentType == null || contentType.isBlank()) {
                return MediaType.APPLICATION_OCTET_STREAM;
            }

            return MediaType.parseMediaType(contentType);
        } catch (IOException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
