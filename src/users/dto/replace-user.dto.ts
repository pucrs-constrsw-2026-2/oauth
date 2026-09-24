import { ApiProperty, ApiPropertyOptional } from "@nestjs/swagger";
import {
  IsBoolean,
  IsString,
  Matches,
  MaxLength,
  MinLength,
  ValidateIf,
} from "class-validator";
import { Transform } from "class-transformer";
import { RFC_5322_EMAIL, RFC_5322_MESSAGE } from "./rfc5322";
import { Trim } from "./trim";

/**
 * Substituição completa. Sem `password`: o Admin API só honra `credentials` na
 * criação — troca de senha é `PATCH`, que a roteia para `reset-password`.
 */
export class ReplaceUserDto {
  @ApiProperty({ example: "ana.souza@pucrs.br" })
  @Trim()
  @IsString({ message: "username é obrigatório" })
  @MinLength(1, { message: "username não pode ser vazio" })
  @MaxLength(255, { message: "username deve ter no máximo 255 caracteres" })
  username!: string;

  @ApiProperty({ example: "ana.souza@pucrs.br" })
  @Trim()
  @IsString({ message: "email é obrigatório" })
  @Matches(RFC_5322_EMAIL, { message: RFC_5322_MESSAGE })
  email!: string;

  @ApiProperty({ example: "Ana" })
  @Transform(({ value, obj }) => value ?? obj?.firstName)
  @Trim()
  @IsString({ message: "first-name é obrigatório" })
  @MinLength(1, { message: "first-name não pode ser vazio" })
  @MaxLength(255, { message: "first-name deve ter no máximo 255 caracteres" })
  "first-name"!: string;

  @ApiProperty({ example: "Souza" })
  @Transform(({ value, obj }) => value ?? obj?.lastName)
  @Trim()
  @IsString({ message: "last-name é obrigatório" })
  @MinLength(1, { message: "last-name não pode ser vazio" })
  @MaxLength(255, { message: "last-name deve ter no máximo 255 caracteres" })
  "last-name"!: string;

  @ApiPropertyOptional({
    default: true,
    description:
      "Ausente equivale a true — um PUT de rotina reabilita um usuário excluído logicamente.",
  })
  @ValidateIf((_object, value) => value !== undefined)
  @IsBoolean({ message: "enabled deve ser booleano" })
  enabled?: boolean;
}
