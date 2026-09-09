import { Global, Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';

import { keycloakConfig } from './keycloak.config';
import { KeycloakSettingsService } from './keycloak-settings.service';

/**
 * `ignoreEnvFile: true` on purpose: the runtime environment is injected by the
 * professor's docker-compose. This service must never grow its own .env file.
 */
@Global()
@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      ignoreEnvFile: true,
      load: [keycloakConfig],
      cache: true,
    }),
  ],
  providers: [KeycloakSettingsService],
  exports: [KeycloakSettingsService],
})
export class AppConfigModule {}
