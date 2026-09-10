export const CANONICAL_ROLE_NAMES = [
  'administrator',
  'coordinator',
  'professor',
  'student',
] as const;

export interface KeycloakRole {
  id: string;
  name: string;
  description?: string;
  clientRole?: boolean;
  containerId?: string;
  attributes?: Record<string, string[]>;
  [key: string]: unknown;
}

export interface RoleInput {
  name?: unknown;
  description?: unknown;
  attributes?: unknown;
}

export interface RoleAssignmentInput {
  roleId?: unknown;
  roleName?: unknown;
  name?: unknown;
}

export interface RoleApiErrorBody {
  error_code: string;
  error_description: string;
  error_source: 'OAuthAPI';
  error_stack: Array<Record<string, unknown>>;
}