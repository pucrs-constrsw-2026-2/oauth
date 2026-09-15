import { z } from "zod";

const environmentSchema = z.object({
  PORT: z.coerce.number().int().min(1).max(65535),
  NODE_ENV: z.enum(["development", "test", "production"]),
  KEYCLOAK_URL: z.string().url(),
  KEYCLOAK_REALM: z.string().min(1),
  KEYCLOAK_CLIENT_ID: z.string().min(1),
  KEYCLOAK_CLIENT_SECRET: z.string().min(1),
  KEYCLOAK_TIMEOUT_MS: z.coerce.number().int().positive(),
  // Admin credentials used to reach the Keycloak Admin REST API (roles CRUD +
  // role-mappings). Obtained via the `admin-cli` client on the `master` realm.
  KEYCLOAK_ADMIN: z.string().min(1),
  KEYCLOAK_ADMIN_PASSWORD: z.string().min(1),
  KEYCLOAK_ADMIN_REALM: z.string().min(1).default("master"),
  KEYCLOAK_ADMIN_CLIENT_ID: z.string().min(1).default("admin-cli"),
  SESSION_COOKIE_NAME: z.string().min(1),
  COOKIE_SECURE: z.enum(["true", "false"]),
  COOKIE_SAME_SITE: z.enum(["lax", "strict", "none"]),
});

export function validateEnvironment(environment: Record<string, unknown>) {
  return environmentSchema.parse(environment);
}

export type Environment = z.infer<typeof environmentSchema>;
