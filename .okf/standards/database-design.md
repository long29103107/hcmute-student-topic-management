# Database Design Standard

- MySQL is the only required database provider for this project.
- Table and column names use `snake_case`; Java fields use `camelCase`.
- Foreign keys, unique constraints and check-like invariants should be encoded
  in schema where MySQL support is reliable, and always rechecked in Service.
- Store timestamps in a consistent timezone strategy and convert only at the
  presentation boundary. The selected strategy must be recorded in config.
- Store report metadata in MySQL and file bytes in a configured storage path;
  do not store unbounded uploads in session or JSP.
- Schema changes must be reproducible from Maven resources/scripts and must not
  rely on manually editing a developer database.
- Do not add another database provider or ORM; JDBC/MySQL is the required
  persistence path.
