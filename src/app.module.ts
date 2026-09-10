import { Module } from "@nestjs/common";
import { ConfigModule } from "@nestjs/config";
import { AuthModule } from "./auth/auth.module";
import { HealthController } from "./health.controller";
import { validateEnvironment } from "./config/env.config";

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true, validate: validateEnvironment }),
    AuthModule,
  ],
  controllers: [HealthController],
})
export class AppModule {}
