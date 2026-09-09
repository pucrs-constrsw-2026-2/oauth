import { Injectable } from '@nestjs/common';
import { OAuthApiException } from '../common/exceptions/oauth-api.exception';
import { KeycloakClientService } from '../common/keycloak/keycloak-client.service';
import { LoginDto } from './dto/login.dto';
import { TokenResponseDto } from './dto/token-response.dto';

@Injectable()
export class AuthService {
  constructor(private readonly keycloak: KeycloakClientService) {}

  async login(dto: LoginDto): Promise<TokenResponseDto> {
    try {
      const token = await this.keycloak.passwordGrant(
        dto.username,
        dto.password,
      );
      return token as unknown as TokenResponseDto;
    } catch (error) {
      if (error instanceof OAuthApiException) {
        const status = error.getStatus();
        // O endpoint de token do Keycloak segue o RFC 6749 e devolve 400
        // (invalid_grant) para credencial invalida - NAO 401. Como a
        // estrutura da chamada ja foi validada pelo LoginDto antes de
        // chegarmos aqui, qualquer erro que o Keycloak devolva neste ponto
        // so pode ser credencial invalida; mapeamos para o 401 exigido pelo
        // enunciado. 503 (Keycloak fora do ar) e preservado como esta.
        if (status !== 503) {
          const original = error.getResponse() as {
            error_stack: unknown[];
          };
          throw new OAuthApiException(
            401,
            'username e/ou password invalidos.',
            original.error_stack as never,
          );
        }
      }
      throw error;
    }
  }
}
