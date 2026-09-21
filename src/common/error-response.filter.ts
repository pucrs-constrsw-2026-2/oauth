import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
  Logger,
} from "@nestjs/common";
import type { Response } from "express";
import { AppError, ErrorStackEntry } from "./errors";

/**
 * Envelope de erro único do serviço, fechado pelo grupo no Sprint 0.
 * Toda rota das quatro trilhas responde falha exatamente nestas quatro chaves.
 */
export interface ErrorResponseBody {
  error_code: string;
  error_description: string;
  error_source: string;
  error_stack: ErrorStackEntry[];
}

const GENERIC_DESCRIPTION = "Erro interno no serviço de identidade.";

@Catch()
export class ErrorResponseFilter implements ExceptionFilter {
  private readonly logger = new Logger(ErrorResponseFilter.name);

  catch(exception: unknown, host: ArgumentsHost) {
    const response = host.switchToHttp().getResponse<Response>();
    const { status, body } = this.describe(exception);
    // `@Catch()` engole a exceção antes do handler padrão do Nest: se não
    // logarmos aqui, a causa de um 500 em produção some por completo.
    if (status >= 500) this.log(exception);
    response.status(status).type("application/json").send(body);
  }

  private log(exception: unknown) {
    if (exception instanceof Error) {
      this.logger.error(exception.message, exception.stack);
      return;
    }
    this.logger.error(String(exception));
  }

  private describe(exception: unknown): {
    status: number;
    body: ErrorResponseBody;
  } {
    if (exception instanceof AppError) {
      return {
        status: exception.status,
        body: {
          error_code: exception.code,
          error_description: exception.message,
          error_source: exception.source,
          error_stack: [exception.toStackEntry(), ...exception.chain],
        },
      };
    }

    if (exception instanceof HttpException) {
      const status = exception.getStatus();
      const code = `OA-${status}`;
      const details = this.validationDetails(exception, code);
      return {
        status,
        body: {
          error_code: code,
          error_description: details.length
            ? "A requisição é inválida."
            : exception.message,
          error_source: details.length ? "validation" : "oauth",
          error_stack: details,
        },
      };
    }

    return {
      status: HttpStatus.INTERNAL_SERVER_ERROR,
      body: {
        error_code: "OA-500",
        error_description: GENERIC_DESCRIPTION,
        error_source: "oauth",
        error_stack: this.unexpectedDetails(exception),
      },
    };
  }

  /**
   * O `ValidationPipe` embala as mensagens do class-validator num array em
   * `message`. Cada mensagem vira uma entrada da cadeia, para o cliente saber
   * qual campo recusou.
   */
  private validationDetails(
    exception: HttpException,
    code: string,
  ): ErrorStackEntry[] {
    const payload = exception.getResponse();
    if (typeof payload !== "object" || payload === null) return [];
    const messages = (payload as { message?: unknown }).message;
    if (!Array.isArray(messages)) return [];
    return messages.map((message) => ({
      source: "validation",
      code,
      description: String(message),
    }));
  }

  /**
   * Causa de erro não previsto só é exposta fora de produção — em produção o
   * cliente recebe a descrição genérica e o detalhe fica no log.
   */
  private unexpectedDetails(exception: unknown): ErrorStackEntry[] {
    if (process.env.NODE_ENV === "production") return [];
    const description =
      exception instanceof Error ? exception.message : String(exception);
    return [{ source: "runtime", code: "OA-500", description }];
  }
}
