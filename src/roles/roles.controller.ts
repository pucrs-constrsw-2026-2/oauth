import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  Param,
  Patch,
  Post,
  Put,
} from "@nestjs/common";
import { ApiTags } from "@nestjs/swagger";
import { CreateRoleDto } from "./dto/create-role.dto";
import { PatchRoleDto } from "./dto/patch-role.dto";
import { UpdateRoleDto } from "./dto/update-role.dto";
import { RoleResponse } from "./interfaces/role-response.interface";
import { RolesService } from "./roles.service";

@ApiTags("roles")
@Controller("v1/roles")
export class RolesController {
  constructor(private readonly roles: RolesService) {}

  @Post()
  create(@Body() dto: CreateRoleDto): Promise<RoleResponse> {
    return this.roles.create(dto);
  }

  @Get()
  findAll(): Promise<RoleResponse[]> {
    return this.roles.findAll();
  }

  @Get(":id")
  findOne(@Param("id") id: string): Promise<RoleResponse> {
    return this.roles.findOne(id);
  }

  @Put(":id")
  update(
    @Param("id") id: string,
    @Body() dto: UpdateRoleDto,
  ): Promise<RoleResponse> {
    return this.roles.update(id, dto);
  }

  @Patch(":id")
  patch(
    @Param("id") id: string,
    @Body() dto: PatchRoleDto,
  ): Promise<RoleResponse> {
    return this.roles.patch(id, dto);
  }

  @Delete(":id")
  @HttpCode(204)
  remove(@Param("id") id: string): Promise<void> {
    return this.roles.remove(id);
  }

  @Post(":id/users/:userId")
  @HttpCode(204)
  assign(
    @Param("id") id: string,
    @Param("userId") userId: string,
  ): Promise<void> {
    return this.roles.assignToUser(id, userId);
  }

  @Delete(":id/users/:userId")
  @HttpCode(204)
  unassign(
    @Param("id") id: string,
    @Param("userId") userId: string,
  ): Promise<void> {
    return this.roles.removeFromUser(id, userId);
  }
}
