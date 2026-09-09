import { Module } from '@nestjs/common';

import { AuthModule } from './auth';
import { CommonModule } from './common';
import { AppConfigModule } from './config';

@Module({
  imports: [AppConfigModule, CommonModule, AuthModule],
})
export class AppModule {}
