import { HttpException, HttpStatus } from '@nestjs/common';

import { RoleApiErrorBody } from './role.types';

export class RoleApiException extends HttpException {
  constructor(status: HttpStatus, code: string, description: string) {
    const body: RoleApiErrorBody = {
      error_code: code,
      error_description: description,
      error_source: 'OAuthAPI',
      error_stack: [{ code, description }],
    };
    super(body, status);
  }
}

export function roleBadRequest(description: string): RoleApiException {
  return new RoleApiException(HttpStatus.BAD_REQUEST, 'OA-400', description);
}

export function roleNotFound(description: string): RoleApiException {
  return new RoleApiException(HttpStatus.NOT_FOUND, 'OA-404', description);
}

export function roleConflict(description: string): RoleApiException {
  return new RoleApiException(HttpStatus.CONFLICT, 'OA-409', description);
}

export function roleUpstreamFailure(
  status: HttpStatus,
  description: string,
): RoleApiException {
  return new RoleApiException(status, `OA-${status}`, description);
}