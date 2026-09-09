import { Global, Module } from '@nestjs/common';

import { BearerAuthGuard } from './bearer-auth.guard';
import { KeycloakTokenVerifierService } from './keycloak-token-verifier.service';

@Global()
@Module({
  providers: [KeycloakTokenVerifierService, BearerAuthGuard],
  exports: [KeycloakTokenVerifierService, BearerAuthGuard],
})
export class CommonModule {}
