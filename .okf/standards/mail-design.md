# Java Mail Design Standard

Java Mail is an optional Nice to Have adapter, not a prerequisite for the core
topic workflow.

- Keep mail sending behind a `NotificationService`/port; Controllers and JSPs
  must not construct mail sessions directly.
- SMTP host, port, sender and credentials come from runtime configuration.
- Never log passwords, SMTP credentials or full private report contents.
- Use safe templates and escape user-controlled title/name/content values.
- A mail delivery failure must not partially commit a core approval, score or
  publication transaction unless a future task explicitly defines an outbox.
- Tests use a fake mail sender; real SMTP/Tomcat verification is optional and
  must be reported separately.
