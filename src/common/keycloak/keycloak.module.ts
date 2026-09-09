import { HttpModule } from '@nestjs/axios';
import { Module } from '@nestjs/common';
import { KeycloakClientService } from './keycloak-client.service';

@Module({
  imports: [HttpModule],
  providers: [KeycloakClientService],
  exports: [KeycloakClientService],
})
export class KeycloakModule {}
