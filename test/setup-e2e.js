"use strict";
process.env.PORT = process.env.PORT ?? "8088";
process.env.NODE_ENV = "test";
process.env.KEYCLOAK_URL = "http://keycloak.test";
process.env.KEYCLOAK_REALM = "constrsw";
process.env.KEYCLOAK_CLIENT_ID = "oauth";
process.env.KEYCLOAK_CLIENT_SECRET = "secret";
process.env.KEYCLOAK_TIMEOUT_MS = "5000";
process.env.KEYCLOAK_ADMIN = "admin";
process.env.KEYCLOAK_ADMIN_PASSWORD = "secret";
process.env.KEYCLOAK_ADMIN_REALM = "master";
process.env.KEYCLOAK_ADMIN_CLIENT_ID = "admin-cli";
process.env.SESSION_COOKIE_NAME = "session";
process.env.COOKIE_SECURE = "false";
process.env.COOKIE_SAME_SITE = "lax";
process.env.OTEL_ENABLED = "false";
//# sourceMappingURL=setup-e2e.js.map