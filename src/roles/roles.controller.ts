import {
  Body,
  Controller,
  Delete,
  Get,
  Headers,
  HttpCode,
  Param,
  Patch,
  Post,
  Put,
  UnauthorizedException,
} from "@nestjs/common";
import {
  ApiBadRequestResponse,
  ApiBearerAuth,
  ApiConflictResponse,
  ApiCreatedResponse,
  ApiNoContentResponse,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
  ApiTags,
} from "@nestjs/swagger";
import { CreateRoleDto } from "./dto/create-role.dto";
import { PatchRoleDto } from "./dto/patch-role.dto";
import { UpdateRoleDto } from "./dto/update-role.dto";
import { RoleResponse } from "./interfaces/role-response.interface";
import { RolesService } from "./roles.service";

@ApiTags("Roles")
@ApiBearerAuth()
@Controller("roles")
export class RolesController {
  constructor(private readonly roles: RolesService) {}

  @Post()
  @ApiOperation({ summary: "Cria um role" })
  @ApiCreatedResponse({ description: "Role criado" })
  @ApiBadRequestResponse({ description: "Payload inválido" })
  @ApiConflictResponse({ description: "Role já existente" })
  create(
    @Headers("authorization") authorization: string | undefined,
    @Body() dto: CreateRoleDto,
  ): Promise<RoleResponse> {
    this.bearerToken(authorization);
    return this.roles.create(dto);
  }

  @Get()
  @ApiOperation({ summary: "Lista todos os roles" })
  @ApiOkResponse({ description: "Roles cadastrados" })
  findAll(@Headers("authorization") authorization?: string): Promise<RoleResponse[]> {
    this.bearerToken(authorization);
    return this.roles.findAll();
  }

  @Get(":id")
  @ApiOperation({ summary: "Busca um role por id" })
  @ApiOkResponse({ description: "Role encontrado" })
  @ApiNotFoundResponse({ description: "Role não encontrado" })
  findOne(
    @Headers("authorization") authorization: string | undefined,
    @Param("id") id: string,
  ): Promise<RoleResponse> {
    this.bearerToken(authorization);
    return this.roles.findOne(id);
  }

  @Put(":id")
  @ApiOperation({ summary: "Atualiza um role" })
  @ApiOkResponse({ description: "Role atualizado" })
  @ApiBadRequestResponse({ description: "Payload inválido" })
  @ApiNotFoundResponse({ description: "Role não encontrado" })
  update(
    @Headers("authorization") authorization: string | undefined,
    @Param("id") id: string,
    @Body() dto: UpdateRoleDto,
  ): Promise<RoleResponse> {
    this.bearerToken(authorization);
    return this.roles.update(id, dto);
  }

  @Patch(":id")
  @ApiOperation({ summary: "Atualiza parcialmente um role" })
  @ApiOkResponse({ description: "Role atualizado" })
  @ApiBadRequestResponse({ description: "Payload inválido" })
  @ApiNotFoundResponse({ description: "Role não encontrado" })
  patch(
    @Headers("authorization") authorization: string | undefined,
    @Param("id") id: string,
    @Body() dto: PatchRoleDto,
  ): Promise<RoleResponse> {
    this.bearerToken(authorization);
    return this.roles.patch(id, dto);
  }

  @Delete(":id")
  @HttpCode(204)
  @ApiOperation({ summary: "Exclui logicamente um role" })
  @ApiNoContentResponse({ description: "Role desabilitado" })
  @ApiNotFoundResponse({ description: "Role não encontrado" })
  delete(
    @Headers("authorization") authorization: string | undefined,
    @Param("id") id: string,
  ): Promise<void> {
    this.bearerToken(authorization);
    return this.roles.delete(id);
  }

  @Post(":id/users/:userId")
  @HttpCode(204)
  @ApiOperation({ summary: "Atribui um role a um usuário" })
  @ApiNoContentResponse({ description: "Role atribuído" })
  @ApiNotFoundResponse({ description: "Role ou usuário não encontrado" })
  assign(
    @Headers("authorization") authorization: string | undefined,
    @Param("id") id: string,
    @Param("userId") userId: string,
  ): Promise<void> {
    this.bearerToken(authorization);
    return this.roles.assignToUser(id, userId);
  }

  @Delete(":id/users/:userId")
  @HttpCode(204)
  @ApiOperation({ summary: "Remove um role de um usuário" })
  @ApiNoContentResponse({ description: "Atribuição removida" })
  @ApiNotFoundResponse({ description: "Role ou usuário não encontrado" })
  unassign(
    @Headers("authorization") authorization: string | undefined,
    @Param("id") id: string,
    @Param("userId") userId: string,
  ): Promise<void> {
    this.bearerToken(authorization);
    return this.roles.removeFromUser(id, userId);
  }

  private bearerToken(authorization?: string): string {
    const match = authorization?.match(/^Bearer\s+([^\s]+)$/i);
    if (!match) throw new UnauthorizedException();
    return match[1];
  }
}
