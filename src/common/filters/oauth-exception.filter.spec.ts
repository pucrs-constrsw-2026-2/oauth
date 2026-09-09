import { ArgumentsHost, BadRequestException, NotFoundException } from '@nestjs/common';
import { OAuthExceptionFilter } from './oauth-exception.filter';
import { OAuthApiException } from '../exceptions/oauth-api.exception';

function buildHost() {
  const json = jest.fn();
  const status = jest.fn().mockReturnValue({ json });
  const response = { status };
  const request = { method: 'GET', url: '/test' };
  const host = {
    switchToHttp: () => ({ getResponse: () => response, getRequest: () => request }),
  } as unknown as ArgumentsHost;
  return { host, status, json };
}

describe('OAuthExceptionFilter', () => {
  let filter: OAuthExceptionFilter;

  beforeEach(() => {
    filter = new OAuthExceptionFilter();
  });

  it('repassa o body de uma OAuthApiException sem alteracoes', () => {
    const { host, status, json } = buildHost();
    const exception = new OAuthApiException(404, 'Usuario nao encontrado.', [
      { error_code: '404', error_description: 'not found', error_source: 'Keycloak' },
    ]);

    filter.catch(exception, host);

    expect(status).toHaveBeenCalledWith(404);
    expect(json).toHaveBeenCalledWith({
      error_code: '404',
      error_description: 'Usuario nao encontrado.',
      error_source: 'OAuthAPI',
      error_stack: [{ error_code: '404', error_description: 'not found', error_source: 'Keycloak' }],
    });
  });

  it('normaliza uma HttpException padrao do Nest (ex.: NotFoundException) para o envelope do T1', () => {
    const { host, status, json } = buildHost();
    const exception = new NotFoundException('Rota nao encontrada');

    filter.catch(exception, host);

    expect(status).toHaveBeenCalledWith(404);
    expect(json).toHaveBeenCalledWith({
      error_code: '404',
      error_description: 'Rota nao encontrada',
      error_source: 'OAuthAPI',
      error_stack: [
        { error_code: '404', error_description: 'Rota nao encontrada', error_source: 'OAuthAPI' },
      ],
    });
  });

  it('junta as mensagens de erro de validacao do ValidationPipe (array) com "; "', () => {
    const { host, status, json } = buildHost();
    const exception = new BadRequestException({
      statusCode: 400,
      message: ['username e obrigatorio.', 'password e obrigatorio.'],
      error: 'Bad Request',
    });

    filter.catch(exception, host);

    expect(status).toHaveBeenCalledWith(400);
    expect(json).toHaveBeenCalledWith(
      expect.objectContaining({
        error_code: '400',
        error_description: 'username e obrigatorio.; password e obrigatorio.',
      }),
    );
  });

  it('normaliza um erro nao tratado (nao HttpException) para 500 generico', () => {
    const { host, status, json } = buildHost();
    const exception = new Error('falha inesperada de infraestrutura');

    filter.catch(exception, host);

    expect(status).toHaveBeenCalledWith(500);
    expect(json).toHaveBeenCalledWith(
      expect.objectContaining({
        error_code: '500',
        error_description: 'Erro interno inesperado na API oauth.',
        error_source: 'OAuthAPI',
      }),
    );
  });
});
