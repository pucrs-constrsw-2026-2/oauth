import { Module } from '@nestjs/common';

import { KeycloakTokenClient } from './keycloak-token.client';
import { LoginController } from './login.controller';

@Module({
  controllers: [LoginController],
  providers: [KeycloakTokenClient],
  exports: [KeycloakTokenClient],
})
export class AuthModule {}
