export class KeycloakDependencyError extends Error {
  constructor(public readonly reason: string, public readonly upstreamStatus?: number) {
    super(reason);
  }
}