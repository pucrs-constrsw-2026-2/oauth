import { ArgumentsHost, Catch, ExceptionFilter, HttpException, HttpStatus } from '@nestjs/common';
import type { Request, Response } from 'express';
import { KeycloakDependencyError } from './errors';

// Upstream (Keycloak Admin API) statuses we surface to the client as-is.
// Anything else from a dependency is reported as 503 (dependency failure).
const SURFACED_UPSTREAM_STATUSES = new Set([400, 401, 403, 404, 409]);

const DETAIL_BY_STATUS: Record<number, string> = {
  400: 'A requisição é inválida.',
  401: 'Credenciais inválidas.',
  403: 'Acesso negado.',
  404: 'Recurso não encontrado.',
  409: 'Conflito com o estado atual do recurso.',
};

@Catch()
export class ProblemDetailsFilter implements ExceptionFilter {
  catch(exception: unknown, host: ArgumentsHost) {
    const response = host.switchToHttp().getResponse<Response>();
    const request = host.switchToHttp().getRequest<Request>();
    let status = HttpStatus.INTERNAL_SERVER_ERROR;
    let code = 'OA-500';
    let detail = 'Erro interno no serviço de identidade.';

    if (exception instanceof KeycloakDependencyError) {
      if (exception.reason === 'invalid_credentials') {
        status = 401;
      } else if (
        exception.upstreamStatus &&
        SURFACED_UPSTREAM_STATUSES.has(exception.upstreamStatus)
      ) {
        status = exception.upstreamStatus;
      } else {
        status = 503;
      }
      code = `OA-${status}`;
      detail =
        status === 503
          ? 'O provedor de identidade está indisponível.'
          : (DETAIL_BY_STATUS[status] ?? 'A requisição não pôde ser processada.');
    } else if (exception instanceof HttpException) {
      status = exception.getStatus();
      code = `OA-${status}`;
      detail =
        DETAIL_BY_STATUS[status] ?? 'A requisição não pôde ser processada.';
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
