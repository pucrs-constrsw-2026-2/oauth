import { ApiPropertyOptional } from "@nestjs/swagger";
import {
  IsBoolean,
  IsString,
  Matches,
  MaxLength,
  MinLength,
  ValidateIf,
} from "class-validator";
import { RFC_5322_EMAIL, RFC_5322_MESSAGE } from "./rfc5322";
import { Trim } from "./trim";

/**
 * Alteração parcial: todo campo é opcional, mas ao menos um é obrigatório —
 * o corpo vazio é recusado no serviço.
 *
 * `@ValidateIf` no lugar de `@IsOptional` de propósito: `@IsOptional` também
 * ignora `null`, então `{"password": null}` escaparia do `@MinLength` e
 * chegaria cru ao Keycloak.
 */
export class PatchUserDto {
  @ApiPropertyOptional({ example: "ana.souza@pucrs.br" })
  @ValidateIf((_object, value) => value !== undefined)
  @Trim()
  @IsString({ message: "username deve ser texto" })
  @MinLength(1, { message: "username não pode ser vazio" })
  @MaxLength(255, { message: "username deve ter no máximo 255 caracteres" })
  username?: string;

  @ApiPropertyOptional({ example: "ana.souza@pucrs.br" })
  @ValidateIf((_object, value) => value !== undefined)
  @Trim()
  @IsString({ message: "email deve ser texto" })
  @Matches(RFC_5322_EMAIL, { message: RFC_5322_MESSAGE })
  email?: string;

  @ApiPropertyOptional({ example: "Ana" })
  @ValidateIf((_object, value) => value !== undefined)
  @Trim()
  @IsString({ message: "firstName deve ser texto" })
  @MinLength(1, { message: "firstName não pode ser vazio" })
  @MaxLength(255, { message: "firstName deve ter no máximo 255 caracteres" })
  firstName?: string;

  @ApiPropertyOptional({ example: "Souza" })
  @ValidateIf((_object, value) => value !== undefined)
  @Trim()
  @IsString({ message: "lastName deve ser texto" })
  @MinLength(1, { message: "lastName não pode ser vazio" })
  @MaxLength(255, { message: "lastName deve ter no máximo 255 caracteres" })
  lastName?: string;

  @ApiPropertyOptional({ example: "senha-nova", minLength: 6 })
  @ValidateIf((_object, value) => value !== undefined)
  @IsString({ message: "password deve ser texto" })
  @MinLength(6, { message: "password deve ter ao menos 6 caracteres" })
  password?: string;

  @ApiPropertyOptional()
  @ValidateIf((_object, value) => value !== undefined)
  @IsBoolean({ message: "enabled deve ser booleano" })
  enabled?: boolean;
}
