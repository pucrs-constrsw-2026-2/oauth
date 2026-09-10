import { Global, Module } from '@nestjs/common';

import { AdministratorRoleGuard } from './administrator-role.guard';
import { BearerAuthGuard } from './bearer-auth.guard';
import { KeycloakAdminClient } from './keycloak-admin.client';
import { KeycloakTokenVerifierService } from './keycloak-token-verifier.service';

@Global()
@Module({
  providers: [
    KeycloakTokenVerifierService,
    KeycloakAdminClient,
    BearerAuthGuard,
    AdministratorRoleGuard,
  ],
  exports: [
    KeycloakTokenVerifierService,
    KeycloakAdminClient,
    BearerAuthGuard,
    AdministratorRoleGuard,
  ],
})
export class CommonModule {}
