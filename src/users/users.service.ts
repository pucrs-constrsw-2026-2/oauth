import { Injectable } from "@nestjs/common";
import { KeycloakError, ValidationError } from "../common/errors";
import { KeycloakAdminClient } from "../keycloak/keycloak-admin.client";
import { CreateUserDto } from "./dto/create-user.dto";
import { PatchUserDto } from "./dto/patch-user.dto";
import { ReplaceUserDto } from "./dto/replace-user.dto";
import {
  CreatedUser,
  KeycloakUserRepresentation,
} from "./interfaces/user.interface";

@Injectable()
export class UsersService {
  constructor(private readonly admin: KeycloakAdminClient) {}

  async create(input: CreateUserDto): Promise<CreatedUser> {
    const representation: KeycloakUserRepresentation = {
      username: input.username,
      email: input.email,
      firstName: input.firstName,
      lastName: input.lastName,
      enabled: input.enabled ?? true,
      emailVerified: false,
      credentials: [
        { type: "password", value: input.password, temporary: false },
      ],
    };

    const response = await this.admin.post("/users", representation);
    return { id: this.idFromLocation(response.headers.get("location")) };
  }

  /**
   * Substituição do recurso na nossa superfície REST: o DTO exige todos os
   * campos e todos vão no corpo, `enabled` incluso.
   *
   * O upstream, porém, faz merge — `PUT /admin/realms/{r}/users/{id}` só grava
   * o que recebe e preserva o resto (`requiredActions`, `attributes`). Por isso
   * `enabled` é sempre enviado: sem ele um PUT de rotina reativaria em silêncio
   * um usuário excluído logicamente.
   */
  async replace(id: string, input: ReplaceUserDto): Promise<void> {
    const representation: KeycloakUserRepresentation = {
      username: input.username,
      email: input.email,
      firstName: input.firstName,
      lastName: input.lastName,
      enabled: input.enabled ?? true,
      // Endereço novo não herda a verificação do antigo.
      emailVerified: false,
    };

    await this.admin.put(this.userPath(id), representation);
  }

  /**
   * Alteração parcial. A senha sai do corpo comum: o Admin API não a atualiza
   * por `PUT /users/{id}` — `credentials` só é honrado na criação. O verbo
   * `PATCH` da nossa superfície REST vira, no upstream, até duas chamadas.
   *
   * A senha vai primeiro: é a que a política de senha do realm costuma recusar,
   * e falhar antes de tocar no perfil deixa o usuário intacto.
   */
  async patch(id: string, input: PatchUserDto): Promise<void> {
    const { password, ...rest } = input;
    const fields = Object.fromEntries(
      Object.entries(rest).filter(
        ([, value]) => value !== undefined && value !== null,
      ),
    ) as KeycloakUserRepresentation;

    if (password === undefined && Object.keys(fields).length === 0) {
      throw new ValidationError(
        "Informe ao menos um campo para alterar.",
        "users",
      );
    }

    if (password !== undefined) {
      await this.admin.put(`${this.userPath(id)}/reset-password`, {
        type: "password",
        value: password,
        temporary: false,
      });
    }

    if (Object.keys(fields).length > 0) {
      if (fields.email !== undefined) fields.emailVerified = false;
      await this.admin.put(this.userPath(id), fields);
    }
  }

  /** Exclusão lógica: o usuário continua no realm, desabilitado. */
  async deactivate(id: string): Promise<void> {
    await this.admin.put(this.userPath(id), { enabled: false });
  }

  private userPath(id: string): string {
    return `/users/${encodeURIComponent(id)}`;
  }

  /**
   * O Admin API devolve o id só no header `Location` do `201`. Se um proxy o
   * removeu, o usuário existe mas não temos como nomeá-lo — a mensagem diz isso
   * para o chamador não reprocessar às cegas e colidir com um 409.
   */
  private idFromLocation(location: string | null): string {
    const id = location?.split("/").filter(Boolean).pop();
    if (!id || id === "users") {
      throw new KeycloakError(
        "O usuário pode ter sido criado, mas o provedor de identidade não devolveu o identificador.",
        { source: "keycloak-admin" },
      );
    }
    return id;
  }
}
