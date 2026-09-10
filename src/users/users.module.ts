import { Module } from '@nestjs/common';

import { KeycloakUsersService } from './keycloak-users.service';
import { UsersController } from './users.controller';

@Module({
  controllers: [UsersController],
  providers: [KeycloakUsersService],
  exports: [KeycloakUsersService],
})
export class UsersModule {}
