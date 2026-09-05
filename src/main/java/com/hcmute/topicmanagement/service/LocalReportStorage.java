package com.hcmute.topicmanagement.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.hcmute.topicmanagement.config.ReportUploadProperties;

@Component
public class LocalReportStorage implements ReportStorage {

    private final Path rootDirectory;

    public LocalReportStorage(ReportUploadProperties properties) {
        this.rootDirectory = properties.getStorageDirectory();
    }

    @Override
    public StoredReport store(MultipartFile file) {
        String storedName = UUID.randomUUID().toString();
        Path temporaryPath = null;
        Path destination = resolve(storedName);
        try {
            Files.createDirectories(rootDirectory);
            temporaryPath = Files.createTempFile(rootDirectory, ".report-", ".tmp");
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, temporaryPath, StandardCopyOption.REPLACE_EXISTING);
            }
            moveIntoPlace(temporaryPath, destination);
            temporaryPath = null;
            return new StoredReport(storedName);
        } catch (IOException exception) {
            deleteQuietly(temporaryPath);
            deleteQuietly(destination);
            throw new ReportStorageException("The report file could not be stored.", exception);
        }
    }

    @Override
    public void delete(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException exception) {
            throw new ReportStorageException("The stored report could not be cleaned up.", exception);
        }
    }

    private void moveIntoPlace(Path temporaryPath, Path destination) throws IOException {
        try {
            Files.move(temporaryPath, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryPath, destination);
        }
    }

    private Path resolve(String storedName) {
        Path candidate = rootDirectory.resolve(storedName).normalize();
        if (!rootDirectory.equals(candidate.getParent())) {
            throw new ReportStorageException("Invalid report storage key.");
        }
        return candidate;
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Preserve the original storage error; cleanup is best effort.
        }
    }
}
