import { ArgumentsHost, Catch, ExceptionFilter } from '@nestjs/common';
import type { Response } from 'express';

import { OaErrorMapper } from './oa-error.mapper';

/**
 * Registered globally in `main.ts` so no route can fall back to Nest's
 * default `{ statusCode, message }` body (Story 3.1, AC #4). Feature modules
 * throw `OaException` (via `OaErrorMapper`) or a standard Nest `HttpException`
 * — both come out the same shape.
 */
@Catch()
export class OaExceptionFilter implements ExceptionFilter {
  catch(exception: unknown, host: ArgumentsHost): void {
    const response = host.switchToHttp().getResponse<Response>();
    const { status, body } = OaErrorMapper.toResponse(exception);

    response.status(status).json(body);
  }
}
