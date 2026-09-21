import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
} from "@nestjs/common";
import type { Response } from "express";
import { KeycloakDependencyError } from "./errors";

interface ErrorStackEntry {
  error_code: string;
  error_description: string;
  error_source: string;
}

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
    let status = HttpStatus.INTERNAL_SERVER_ERROR;
    let code = "OA-500";
    let detail = "Erro interno no serviço de identidade.";
    const errorStack: ErrorStackEntry[] = [];

    if (exception instanceof KeycloakDependencyError) {
      status = exception.reason === "invalid_credentials"
        ? 401
        : exception.upstreamStatus && SURFACED_UPSTREAM_STATUSES.has(exception.upstreamStatus)
          ? exception.upstreamStatus
          : 503;
      code = `OA-${status}`;
      detail = status === 503
        ? "O provedor de identidade está indisponível."
        : (DETAIL_BY_STATUS[status] ?? "A requisição não pôde ser processada.");
      errorStack.push({
        error_code: String(status),
        error_description: "Erro retornado pelo Keycloak.",
        error_source: "Keycloak",
      });
    } else if (exception instanceof HttpException) {
      status = exception.getStatus();
      code = `OA-${status}`;
      detail = status === 401
        ? "O access token é obrigatório ou inválido."
        : DETAIL_BY_STATUS[status] ?? "A requisição não pôde ser processada.";
    }

    errorStack.push({
      error_code: code,
      error_description: detail,
      error_source: "OAuthAPI",
    });

    response.status(status).type("application/json").send({
      error_code: code,
      error_description: detail,
      error_source: "OAuthAPI",
      error_stack: errorStack,
    });
  }
}
