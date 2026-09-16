import { Controller, Get } from "@nestjs/common";
import type { HealthResponse } from "./interfaces/health-response.interface";

@Controller("health")
export class HealthController {
  @Get()
  check(): HealthResponse {
    return { status: "ok", service: "oauth" };
  }
}
