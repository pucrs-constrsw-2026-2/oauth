import {
  BadRequestException,
  Controller,
  Get,
  Headers,
  Param,
  ParseBoolPipe,
  Query,
  UnauthorizedException,
} from "@nestjs/common";
import { UsersService } from "./users.service";

@Controller("users")
export class UsersController {
  constructor(private readonly users: UsersService) {}

  @Get()
  list(
    @Headers("authorization") authorization?: string,
    @Query("enabled", new ParseBoolPipe({ optional: true })) enabled?: boolean,
  ) {
    return this.users.list(this.bearerToken(authorization), enabled);
  }

  @Get(":id")
  get(
    @Headers("authorization") authorization?: string,
    @Param("id") id?: string,
  ) {
    if (!id) throw new BadRequestException();
    return this.users.get(this.bearerToken(authorization), id);
  }

  private bearerToken(authorization?: string): string {
    const match = authorization?.match(/^Bearer\s+([^\s]+)$/i);
    if (!match) throw new UnauthorizedException();
    return match[1];
  }
}
