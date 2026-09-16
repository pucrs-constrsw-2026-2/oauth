import { Module } from "@nestjs/common";
import { ConfigModule } from "@nestjs/config";
import { AuthModule } from "./auth/auth.module";
import { HealthController } from "./health.controller";
import { UsersModule } from "./users/users.module";
import { validateEnvironment } from "./config/env.config";

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true, validate: validateEnvironment }),
    AuthModule,
    UsersModule,
  ],
  controllers: [HealthController],
})
export class AppModule {}
