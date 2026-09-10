import { Module } from '@nestjs/common';
import { APP_FILTER } from '@nestjs/core';

import { AuthModule } from './auth';
import { AuthzModule } from './authz';
import { CommonModule } from './common';
import { AppConfigModule } from './config';
import { OaExceptionFilter } from './errors';
import { HealthController } from './health/health.controller';
import { RolesModule } from './roles';
import { UsersModule } from './users';

@Module({
  imports: [
    AppConfigModule,
    CommonModule,
    AuthModule,
    AuthzModule,
    RolesModule,
    UsersModule,
  ],
  controllers: [HealthController],
  // Registered through APP_FILTER rather than useGlobalFilters so the filter is
  // built by the DI container and every route - including ones added later -
  // answers with the same OA envelope.
  providers: [{ provide: APP_FILTER, useClass: OaExceptionFilter }],
})
export class AppModule {}
