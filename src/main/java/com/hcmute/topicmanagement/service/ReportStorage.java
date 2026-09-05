package com.hcmute.topicmanagement.service;

import org.springframework.web.multipart.MultipartFile;

public interface ReportStorage {

    StoredReport store(MultipartFile file);

    void delete(String storedName);

    record StoredReport(String storedName) {
    }

    class ReportStorageException extends RuntimeException {
        public ReportStorageException(String message, Throwable cause) {
            super(message, cause);
        }

        public ReportStorageException(String message) {
            super(message);
        }
    }
}
