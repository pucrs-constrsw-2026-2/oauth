import { Module } from '@nestjs/common';

import { AuthzController } from './authz.controller';
import { KeycloakAuthorizationClient } from './keycloak-authorization.client';

@Module({
  controllers: [AuthzController],
  providers: [KeycloakAuthorizationClient],
})
export class AuthzModule {}
