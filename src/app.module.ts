import { Module } from '@nestjs/common';

import { CommonModule } from './common';
import { AppConfigModule } from './config';

@Module({
  imports: [AppConfigModule, CommonModule],
})
export class AppModule {}
