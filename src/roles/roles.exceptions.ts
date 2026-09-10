import { HttpStatus } from '@nestjs/common';

import { OaException, OaErrorMapper } from '../errors';

/**
 * Role failures use the shared OA envelope from Story 3.1 rather than a
 * private one. A local `HttpException` carrying an already-built body would
 * lose its description in the global filter, which reads `message`.
 */

export function roleBadRequest(description: string): OaException {
  return OaErrorMapper.badRequest(description);
}

export function roleNotFound(description: string): OaException {
  return OaErrorMapper.notFound(description);
}

export function roleConflict(description: string): OaException {
  return OaErrorMapper.conflict(description);
}

export function roleUpstreamFailure(
  status: HttpStatus,
  description: string,
): OaException {
  return OaErrorMapper.fromStatus(status, description);
}
