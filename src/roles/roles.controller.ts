import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  Patch,
  Post,
  Put,
  UseGuards,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOperation,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';
import { BearerToken } from '../common/decorators/bearer-token.decorator';
import { ErrorResponseDto } from '../common/dto/error-response.dto';
import { BearerTokenGuard } from '../common/guards/bearer-token.guard';
import { CreateRoleDto } from './dto/create-role.dto';
import { PatchRoleDto } from './dto/patch-role.dto';
import { RoleResponseDto } from './dto/role-response.dto';
import { UpdateRoleDto } from './dto/update-role.dto';
import { RolesService } from './roles.service';

@ApiTags('roles')
@ApiBearerAuth()
@UseGuards(BearerTokenGuard)
@Controller('roles')
export class RolesController {
  constructor(private readonly rolesService: RolesService) {}

  @Post()
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Cria um role' })
  @ApiResponse({ status: 201, type: RoleResponseDto })
  @ApiResponse({ status: 400, type: ErrorResponseDto })
  @ApiResponse({ status: 401, type: ErrorResponseDto })
  @ApiResponse({ status: 403, type: ErrorResponseDto })
  @ApiResponse({ status: 409, type: ErrorResponseDto })
  async create(
    @BearerToken() token: string,
    @Body() dto: CreateRoleDto,
  ): Promise<RoleResponseDto> {
    return this.rolesService.create(token, dto);
  }

  @Get()
  @ApiOperation({ summary: 'Lista todos os roles' })
  @ApiResponse({ status: 200, type: [RoleResponseDto] })
  @ApiResponse({ status: 401, type: ErrorResponseDto })
  @ApiResponse({ status: 403, type: ErrorResponseDto })
  async findAll(@BearerToken() token: string): Promise<RoleResponseDto[]> {
    return this.rolesService.findAll(token);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Recupera um role pelo id' })
  @ApiResponse({ status: 200, type: RoleResponseDto })
  @ApiResponse({ status: 401, type: ErrorResponseDto })
  @ApiResponse({ status: 403, type: ErrorResponseDto })
  @ApiResponse({ status: 404, type: ErrorResponseDto })
  async findOne(
    @BearerToken() token: string,
    @Param('id') id: string,
  ): Promise<RoleResponseDto> {
    return this.rolesService.findOne(token, id);
  }

  @Put(':id')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Atualiza um role (completo)' })
  @ApiResponse({ status: 200, description: 'OK (vazio)' })
  @ApiResponse({ status: 400, type: ErrorResponseDto })
  @ApiResponse({ status: 401, type: ErrorResponseDto })
  @ApiResponse({ status: 403, type: ErrorResponseDto })
  @ApiResponse({ status: 404, type: ErrorResponseDto })
  async update(
    @BearerToken() token: string,
    @Param('id') id: string,
    @Body() dto: UpdateRoleDto,
  ): Promise<void> {
    await this.rolesService.replace(token, id, dto);
  }

  @Patch(':id')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Atualiza um role (parcial)' })
  @ApiResponse({ status: 200, description: 'OK (vazio)' })
  @ApiResponse({ status: 400, type: ErrorResponseDto })
  @ApiResponse({ status: 401, type: ErrorResponseDto })
  @ApiResponse({ status: 403, type: ErrorResponseDto })
  @ApiResponse({ status: 404, type: ErrorResponseDto })
  async patch(
    @BearerToken() token: string,
    @Param('id') id: string,
    @Body() dto: PatchRoleDto,
  ): Promise<void> {
    await this.rolesService.patch(token, id, dto);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Exclusao de um role' })
  @ApiResponse({ status: 204, description: 'No Content' })
  @ApiResponse({ status: 401, type: ErrorResponseDto })
  @ApiResponse({ status: 403, type: ErrorResponseDto })
  @ApiResponse({ status: 404, type: ErrorResponseDto })
  async remove(
    @BearerToken() token: string,
    @Param('id') id: string,
  ): Promise<void> {
    await this.rolesService.remove(token, id);
  }
}
