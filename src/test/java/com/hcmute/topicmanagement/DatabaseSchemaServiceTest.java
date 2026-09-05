package com.hcmute.topicmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import com.hcmute.topicmanagement.service.DatabaseSchemaService;
import com.hcmute.topicmanagement.service.SeedStepResult;

class DatabaseSchemaServiceTest {

    @Test
    void recreateSchemaDropsAllProjectTablesBeforeExecutingDdl() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        Statement dropStatement = mock(Statement.class);
        Statement ddlStatement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("MySQL");
        when(connection.createStatement()).thenReturn(dropStatement, ddlStatement);

        SeedStepResult result = new DatabaseSchemaService(dataSource).recreateSchema();

        verify(dropStatement).execute("SET FOREIGN_KEY_CHECKS = 0");
        verify(dropStatement).execute("DROP TABLE IF EXISTS `registration_results`, `evaluations`, "
                + "`review_board_members`, `review_boards`, `reports`, `topic_registrations`, "
                + "`group_members`, `topic_supervisors`, `student_groups`, `topics`, "
                + "`registration_periods`, `departments`, `user_roles`, `role_permissions`, "
                + "`users`, `permissions`, `roles`");
        verify(dropStatement).execute("SET FOREIGN_KEY_CHECKS = 1");
        verify(ddlStatement).execute("CREATE DATABASE IF NOT EXISTS hcmute_topic_management\n"
                + "    CHARACTER SET utf8mb4\n"
                + "    COLLATE utf8mb4_unicode_ci");
        verify(ddlStatement).execute("CREATE TABLE IF NOT EXISTS roles (\n"
                + "    id BIGINT NOT NULL AUTO_INCREMENT,\n"
                + "    code VARCHAR(30) NOT NULL,\n"
                + "    name VARCHAR(100) NOT NULL,\n"
                + "    description VARCHAR(255) NULL,\n"
                + "    system_role BOOLEAN NOT NULL DEFAULT TRUE,\n"
                + "    active BOOLEAN NOT NULL DEFAULT TRUE,\n"
                + "    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),\n"
                + "    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),\n"
                + "    CONSTRAINT pk_roles PRIMARY KEY (id),\n"
                + "    CONSTRAINT uk_roles_code UNIQUE (code)\n"
                + ") ENGINE = InnoDB");
        assertThat(result.step()).isEqualTo("ddl");
        assertThat(result.count()).isEqualTo(17);
    }
}
