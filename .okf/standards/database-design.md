# Database Design Standard

`database/1.ddl.sql` is the canonical revised schema and currently contains
the core workflow plus review-board and registration-result tables from the
2026-09-03 DrawSQL export. Keep entity mappings and repository contracts in
sync with that file; do not silently reintroduce columns removed by the export.

- MySQL is the only required database provider for this project.
- Table and column names use `snake_case`; Java fields use `camelCase`.
- Foreign keys, unique constraints and check-like invariants should be encoded
  in schema where MySQL support is reliable, and always rechecked in Service.
- Store timestamps in a consistent timezone strategy and convert only at the
  presentation boundary. The selected strategy must be recorded in config.
- Store report metadata in MySQL and file bytes in a configured storage path;
  do not store unbounded uploads in session or Thymeleaf views.
- Schema changes must be reproducible from Maven resources/scripts and must not
  rely on manually editing a developer database.
- MySQL is the authoritative runtime schema; Spring Data JPA/Hibernate is the
  persistence implementation and H2 is used only by the automated tests.
