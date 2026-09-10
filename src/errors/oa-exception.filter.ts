import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
} from '@nestjs/common';
import type { Request, Response } from 'express';

interface OaErrorBody {
  error_code: string;
  error_description: string;
  error_source: 'OAuthAPI';
  error_stack: Array<Record<string, unknown>>;
}

@Catch()
export class OaExceptionFilter implements ExceptionFilter {
  catch(exception: unknown, host: ArgumentsHost): void {
    const response = host.switchToHttp().getResponse<Response>();
    const request = host.switchToHttp().getRequest<Request>();
    const status = getStatus(exception);
    const description = getDescription(exception, status);
    const body: OaErrorBody = {
      error_code: `OA-${status}`,
      error_description: description,
      error_source: 'OAuthAPI',
      error_stack: [
        {
          method: request.method,
          path: request.originalUrl,
          status,
          description,
        },
      ],
    };

    response.status(status).json(body);
  }
}

function getStatus(exception: unknown): number {
  if (exception instanceof HttpException) {
    return exception.getStatus();
  }
  return HttpStatus.INTERNAL_SERVER_ERROR;
}

function getDescription(exception: unknown, status: number): string {
  if (exception instanceof HttpException) {
    const payload = exception.getResponse();
    if (typeof payload === 'object' && payload !== null) {
      const description = (payload as Record<string, unknown>).error_description;
      if (typeof description === 'string') {
        return description;
      }
      const message = (payload as Record<string, unknown>).message;
      if (typeof message === 'string') {
        return message;
      }
      if (Array.isArray(message)) {
        return message.join('; ');
      }
    }
    if (typeof payload === 'string') {
      return payload;
    }
  }
  return status >= 500 ? 'Unexpected OAuth API failure.' : 'Request failed.';
}