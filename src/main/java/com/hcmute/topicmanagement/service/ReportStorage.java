package com.hcmute.topicmanagement.service;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public interface ReportStorage {

    StoredReport store(MultipartFile file);

    StoredReportContent open(String storedName);

    void delete(String storedName);

    record StoredReport(String storedName) {
    }

    record StoredReportContent(InputStream content, long contentLength) {
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
