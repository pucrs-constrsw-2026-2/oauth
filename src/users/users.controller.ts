import {
  Body,
  Controller,
  Delete,
  HttpCode,
  HttpStatus,
  Param,
  Patch,
  Post,
  Put,
} from "@nestjs/common";
import {
  ApiBadRequestResponse,
  ApiConflictResponse,
  ApiCreatedResponse,
  ApiNoContentResponse,
  ApiNotFoundResponse,
  ApiServiceUnavailableResponse,
  ApiTags,
} from "@nestjs/swagger";
import { CreateUserDto } from "./dto/create-user.dto";
import { PatchUserDto } from "./dto/patch-user.dto";
import { ReplaceUserDto } from "./dto/replace-user.dto";
import { CreatedUser } from "./interfaces/user.interface";
import { UsersService } from "./users.service";

@ApiTags("users")
@ApiBadRequestResponse({
  description: "Corpo inválido; cada recusa vira uma entrada de error_stack.",
})
@ApiNotFoundResponse({ description: "Usuário não encontrado no realm." })
@ApiServiceUnavailableResponse({
  description: "Provedor de identidade indisponível.",
})
@Controller("v1/users")
export class UsersController {
  constructor(private readonly users: UsersService) {}

  @Post()
  @HttpCode(HttpStatus.CREATED)
  @ApiCreatedResponse({ description: "Usuário criado; devolve o id." })
  @ApiConflictResponse({ description: "Username ou e-mail já em uso." })
  create(@Body() input: CreateUserDto): Promise<CreatedUser> {
    return this.users.create(input);
  }

  @Put(":id")
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiNoContentResponse({ description: "Usuário substituído." })
  @ApiConflictResponse({ description: "Username ou e-mail já em uso." })
  replace(
    @Param("id") id: string,
    @Body() input: ReplaceUserDto,
  ): Promise<void> {
    return this.users.replace(id, input);
  }

  @Patch(":id")
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiNoContentResponse({ description: "Usuário alterado." })
  @ApiConflictResponse({ description: "Username ou e-mail já em uso." })
  patch(@Param("id") id: string, @Body() input: PatchUserDto): Promise<void> {
    return this.users.patch(id, input);
  }

  @Delete(":id")
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiNoContentResponse({ description: "Usuário desabilitado (exclusão lógica)." })
  remove(@Param("id") id: string): Promise<void> {
    return this.users.deactivate(id);
  }
}
