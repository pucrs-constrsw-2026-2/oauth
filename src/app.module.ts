import { Module } from '@nestjs/common';
import { APP_FILTER } from '@nestjs/core';

import { CommonModule } from './common';
import { AppConfigModule } from './config';
import { ErrorsModule, OaExceptionFilter } from './errors';
import { RolesModule } from './roles';
import { HealthController } from './health/health.controller';

@Module({
  imports: [AppConfigModule, CommonModule, ErrorsModule, RolesModule],
  controllers: [HealthController],
  providers: [{ provide: APP_FILTER, useClass: OaExceptionFilter }],
})
export class AppModule {}
