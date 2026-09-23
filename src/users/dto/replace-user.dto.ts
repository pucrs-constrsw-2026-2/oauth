import { ApiProperty, ApiPropertyOptional } from "@nestjs/swagger";
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
  @Trim()
  @IsString({ message: "firstName é obrigatório" })
  @MinLength(1, { message: "firstName não pode ser vazio" })
  @MaxLength(255, { message: "firstName deve ter no máximo 255 caracteres" })
  firstName!: string;

  @ApiProperty({ example: "Souza" })
  @Trim()
  @IsString({ message: "lastName é obrigatório" })
  @MinLength(1, { message: "lastName não pode ser vazio" })
  @MaxLength(255, { message: "lastName deve ter no máximo 255 caracteres" })
  lastName!: string;

  @ApiPropertyOptional({
    default: true,
    description:
      "Ausente equivale a true — um PUT de rotina reabilita um usuário excluído logicamente.",
  })
  @ValidateIf((_object, value) => value !== undefined)
  @IsBoolean({ message: "enabled deve ser booleano" })
  enabled?: boolean;
}
