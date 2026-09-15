import { Module } from "@nestjs/common";
import { KeycloakAdminClient } from "./keycloak-admin.client";
import { RolesController } from "./roles.controller";
import { RolesService } from "./roles.service";

@Module({
  controllers: [RolesController],
  providers: [RolesService, KeycloakAdminClient],
})
export class RolesModule {}
