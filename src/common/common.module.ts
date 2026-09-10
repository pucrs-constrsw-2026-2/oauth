import { Global, Module } from '@nestjs/common';

import { AdministratorRoleGuard } from './administrator-role.guard';
import { BearerAuthGuard } from './bearer-auth.guard';
import { KeycloakTokenVerifierService } from './keycloak-token-verifier.service';

@Global()
@Module({
  providers: [KeycloakTokenVerifierService, BearerAuthGuard, AdministratorRoleGuard],
  exports: [KeycloakTokenVerifierService, BearerAuthGuard, AdministratorRoleGuard],
})
export class CommonModule {}
