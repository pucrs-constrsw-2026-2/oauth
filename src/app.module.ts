import { Module } from '@nestjs/common';

import { AuthModule } from './auth';
import { AuthzModule } from './authz';
import { CommonModule } from './common';
import { AppConfigModule } from './config';

@Module({
  imports: [AppConfigModule, CommonModule, AuthModule, AuthzModule],
})
export class AppModule {}
