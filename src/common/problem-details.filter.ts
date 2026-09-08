import { ArgumentsHost, Catch, ExceptionFilter, HttpException, HttpStatus } from '@nestjs/common';
import type { Request, Response } from 'express';
import { KeycloakDependencyError } from './errors';

@Catch()
export class ProblemDetailsFilter implements ExceptionFilter {
  catch(exception: unknown, host: ArgumentsHost) {
    const response = host.switchToHttp().getResponse<Response>();
    const request = host.switchToHttp().getRequest<Request>();
    let status = HttpStatus.INTERNAL_SERVER_ERROR;
    let code = 'OA-500';
    let detail = 'Erro interno no serviço de identidade.';

    if (exception instanceof KeycloakDependencyError) {
      status = exception.reason === 'invalid_credentials' ? 401 : 503;
      code = status === 401 ? 'OA-401' : 'OA-503';
      detail = status === 401 ? 'Credenciais inválidas.' : 'O provedor de identidade está indisponível.';
    } else if (exception instanceof HttpException) {
      status = exception.getStatus();
      code = `OA-${status}`;
      detail = status === 400 ? 'A requisição é inválida.' : 'A requisição não pôde ser processada.';
    }

    response.status(status).type('application/problem+json').send({
      type: `https://docs.constrsw.local/problems/${code.toLowerCase()}`,
      title: status >= 500 ? 'Falha de dependência' : 'Requisição inválida',
      status,
      code,
      detail,
      instance: request.url,
    });
  }
}