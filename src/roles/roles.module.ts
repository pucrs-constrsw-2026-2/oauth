import { Module } from "@nestjs/common";
import { KeycloakModule } from "../keycloak/keycloak.module";
import { RolesController } from "./roles.controller";
import { RolesService } from "./roles.service";

@Module({
  imports: [KeycloakModule],
  controllers: [RolesController],
  providers: [RolesService],
})
export class RolesModule {}
