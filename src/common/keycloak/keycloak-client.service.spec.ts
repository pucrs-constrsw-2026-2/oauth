import { of, throwError } from 'rxjs';
import { AxiosError } from 'axios';
import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { KeycloakClientService } from './keycloak-client.service';
import { OAuthApiException } from '../exceptions/oauth-api.exception';

function buildConfig(overrides: Record<string, string> = {}): ConfigService {
  const values: Record<string, string> = {
    'keycloak.baseUrl': 'http://keycloak:8080/',
    'keycloak.realm': 'constrsw',
    'keycloak.clientId': 'oauth',
    'keycloak.clientSecret': 'segredo',
    ...overrides,
  };
  return { get: (key: string) => values[key] } as unknown as ConfigService;
}

describe('KeycloakClientService', () => {
  it('passwordGrant remove a barra final da baseUrl e monta a URL do token corretamente', async () => {
    const post = jest.fn().mockReturnValue(
      of({
        data: {
          token_type: 'Bearer',
          access_token: 'abc',
          expires_in: 600,
          refresh_token: 'def',
          refresh_expires_in: 1800,
        },
      }),
    );
    const http = { post, request: jest.fn() } as unknown as HttpService;
    const service = new KeycloakClientService(http, buildConfig());

    const token = await service.passwordGrant('admin@pucrs.br', 'a12345678');

    expect(token.access_token).toBe('abc');
    expect(post).toHaveBeenCalledWith(
      'http://keycloak:8080/realms/constrsw/protocol/openid-connect/token',
      expect.stringContaining('grant_type=password'),
      { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } },
    );
  });

  it('adminRequest inclui o Authorization: Bearer com o token do CHAMADOR repassado (nao um token de servico)', async () => {
    const request = jest.fn().mockReturnValue(of({ data: [], headers: {} }));
    const http = { post: jest.fn(), request } as unknown as HttpService;
    const service = new KeycloakClientService(http, buildConfig());

    await service.adminRequest('GET', '/users', 'meu-token');

    expect(request).toHaveBeenCalledWith(
      expect.objectContaining({
        method: 'GET',
        url: 'http://keycloak:8080/admin/realms/constrsw/users',
        headers: { Authorization: 'Bearer meu-token' },
      }),
    );
  });

  it('mapeia erro de rede (sem response, ex.: Keycloak fora do ar) para 503', async () => {
    const axiosError = { message: 'connect ECONNREFUSED' } as unknown as AxiosError;
    const post = jest.fn().mockReturnValue(throwError(() => axiosError));
    const http = { post, request: jest.fn() } as unknown as HttpService;
    const service = new KeycloakClientService(http, buildConfig());

    try {
      await service.passwordGrant('a', 'b');
      throw new Error('deveria ter lancado OAuthApiException');
    } catch (err) {
      const e = err as OAuthApiException;
      expect(e).toBeInstanceOf(OAuthApiException);
      expect(e.getStatus()).toBe(503);
      expect((e.getResponse() as { error_description: string }).error_description).toBe(
        'Nao foi possivel se comunicar com o Keycloak.',
      );
    }
  });

  it('mapeia erro HTTP do Keycloak preservando o status e usando errorMessage como descricao', async () => {
    const axiosError = {
      response: {
        status: 409,
        data: { errorMessage: 'User exists with same email' },
      },
      message: 'Request failed with status code 409',
    } as unknown as AxiosError;
    const request = jest.fn().mockReturnValue(throwError(() => axiosError));
    const http = { post: jest.fn(), request } as unknown as HttpService;
    const service = new KeycloakClientService(http, buildConfig());

    try {
      await service.adminRequest('POST', '/users', 'token');
      throw new Error('deveria ter lancado OAuthApiException');
    } catch (err) {
      const e = err as OAuthApiException;
      expect(e.getStatus()).toBe(409);
      const body = e.getResponse() as {
        error_description: string;
        error_stack: Array<{ error_code: string; error_description: string; error_source: string }>;
      };
      expect(body.error_description).toBe('User exists with same email');
      expect(body.error_stack[0]).toEqual({
        error_code: '409',
        error_description: 'User exists with same email',
        error_source: 'Keycloak',
      });
    }
  });
});
