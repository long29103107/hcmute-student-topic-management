# Security Standard

- Authenticate through a server-side session; invalidate it on logout and on
  session-id rotation where supported.
- Store only a user id and safe display/role information in session.
- Hash and verify passwords with a vetted password hashing algorithm; plaintext
  passwords must never be persisted, logged or put in DTOs.
- Enforce authorization in Service for every mutation and protected read. A
  hidden button is not authorization.
- Use prepared SQL statements and validate sort/filter fields against allowlists.
- Escape JSP output. Avoid putting untrusted values into raw HTML, JavaScript
  or URL attributes without context-aware encoding.
- Validate uploads for size, extension, content type and generated path; store
  outside the public web root when possible.
- Use POST for mutations and protect forms against CSRF when the deployment
  exposes authenticated browser sessions.
- Do not log passwords, session ids, uploaded contents or sensitive connection
  details.
