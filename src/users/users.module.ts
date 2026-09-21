import { Module } from "@nestjs/common";
import { KeycloakClient } from "../auth/keycloak.client";
import { UsersController } from "./users.controller";
import { UsersService } from "./users.service";

@Module({
  controllers: [UsersController],
  providers: [UsersService, KeycloakClient],
})
export class UsersModule {}
