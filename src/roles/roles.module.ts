import { Module } from '@nestjs/common';
import { KeycloakModule } from '../common/keycloak/keycloak.module';
import { RolesController } from './roles.controller';
import { RolesService } from './roles.service';
import { UserRolesController } from './user-roles.controller';

@Module({
  imports: [KeycloakModule],
  controllers: [RolesController, UserRolesController],
  providers: [RolesService],
})
export class RolesModule {}
