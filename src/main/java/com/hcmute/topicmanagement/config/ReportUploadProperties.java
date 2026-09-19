package com.hcmute.topicmanagement.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public final class ReportUploadProperties {

    private final Path storageDirectory;
    private final long maxFileSizeBytes;
    private final int maxFiles;
    private final Set<String> allowedContentTypes;

    public ReportUploadProperties(
            @Value("${reports.storage.directory:storage/reports}") String storageDirectory,
            @Value("${reports.upload.max-size-bytes:10485760}") long maxFileSizeBytes,
            @Value("${reports.upload.max-files:10}") int maxFiles,
            @Value("${reports.upload.allowed-content-types:application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document}") String allowedContentTypes) {
        if (maxFileSizeBytes <= 0) {
            throw new IllegalArgumentException("reports.upload.max-size-bytes must be positive.");
        }
        if (maxFiles <= 0) {
            throw new IllegalArgumentException("reports.upload.max-files must be positive.");
        }
        this.storageDirectory = Paths.get(storageDirectory).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxFiles = maxFiles;
        this.allowedContentTypes = Arrays.stream(allowedContentTypes.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        if (this.allowedContentTypes.isEmpty()) {
            throw new IllegalArgumentException("reports.upload.allowed-content-types must not be empty.");
        }
    }

    public Path getStorageDirectory() {
        return storageDirectory;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    public Set<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }
}
