import { Logger } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';

import { AppModule } from './app.module';
import { KeycloakSettingsService } from './config';

async function bootstrap(): Promise<void> {
  const app = await NestFactory.create(AppModule);
  const settings = app.get(KeycloakSettingsService);

  await app.listen(settings.internalApiPort);

  Logger.log(
    `oauth listening on port ${settings.internalApiPort} — ` +
      `Keycloak realm "${settings.realm}" at ${settings.serverUrl}`,
    'Bootstrap',
  );
}

void bootstrap();
