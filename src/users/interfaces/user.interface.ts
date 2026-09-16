/** Resposta do `POST /v1/users`: só o id, como manda o enunciado. */
export interface CreatedUser {
  id: string;
}

/** Subconjunto da UserRepresentation do Keycloak que a trilha B escreve. */
export interface KeycloakUserRepresentation {
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  enabled?: boolean;
  emailVerified?: boolean;
  credentials?: KeycloakCredential[];
}

export interface KeycloakCredential {
  type: "password";
  value: string;
  temporary: boolean;
}
