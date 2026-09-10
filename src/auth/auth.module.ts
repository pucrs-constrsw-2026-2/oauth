import { Module } from '@nestjs/common';

import { KeycloakTokenClient } from './keycloak-token.client';
import { LoginController } from './login.controller';
import { RefreshController } from './refresh.controller';

@Module({
  controllers: [LoginController, RefreshController],
  providers: [KeycloakTokenClient],
  exports: [KeycloakTokenClient],
})
export class AuthModule {}
