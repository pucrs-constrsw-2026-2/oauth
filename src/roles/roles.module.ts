import { Module } from '@nestjs/common';

import { KeycloakAdminService } from './keycloak-admin.service';
import { RolesController } from './roles.controller';

@Module({
  controllers: [RolesController],
  providers: [KeycloakAdminService],
})
export class RolesModule {}