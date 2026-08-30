package com.hcmute.topicmanagement.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class DatabaseSchemaService {

    private static final String DDL_RESOURCE = "database/1.ddl.sql";

    private final DataSource dataSource;

    public DatabaseSchemaService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public SeedStepResult createSchema() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.getMetaData().getDatabaseProductName().toLowerCase().contains("h2")) {
                return new SeedStepResult("ddl", 0, LocalDateTime.now());
            }

            String script = new ClassPathResource(DDL_RESOURCE)
                    .getContentAsString(StandardCharsets.UTF_8);
            int executedStatements = executeStatements(connection, script);
            return new SeedStepResult("ddl", executedStatements, LocalDateTime.now());
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Cannot create the database schema from " + DDL_RESOURCE + ".", exception);
        }
    }

    private int executeStatements(Connection connection, String script) throws SQLException {
        int executedStatements = 0;
        try (Statement statement = connection.createStatement()) {
            for (String sql : splitStatements(script)) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                    executedStatements++;
                }
            }
        }
        return executedStatements;
    }

    private List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean lineComment = false;
        boolean blockComment = false;

        for (int index = 0; index < script.length(); index++) {
            char character = script.charAt(index);
            char nextCharacter = index + 1 < script.length() ? script.charAt(index + 1) : '\0';

            if (lineComment) {
                if (character == '\n' || character == '\r') {
                    lineComment = false;
                    current.append(character);
                }
                continue;
            }
            if (blockComment) {
                if (character == '*' && nextCharacter == '/') {
                    blockComment = false;
                    index++;
                }
                continue;
            }
            if (!singleQuoted && !doubleQuoted && character == '-' && nextCharacter == '-') {
                lineComment = true;
                index++;
                continue;
            }
            if (!singleQuoted && !doubleQuoted && character == '/' && nextCharacter == '*') {
                blockComment = true;
                index++;
                continue;
            }
            if (character == '\'' && !doubleQuoted) {
                singleQuoted = !singleQuoted;
            } else if (character == '"' && !singleQuoted) {
                doubleQuoted = !doubleQuoted;
            }

            if (character == ';' && !singleQuoted && !doubleQuoted) {
                statements.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        if (!current.toString().isBlank()) {
            statements.add(current.toString().trim());
        }
        return statements;
    }
}
