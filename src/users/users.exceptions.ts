import { OaErrorMapper, OaException } from '../errors';

/**
 * User failures use the shared OA envelope; these helpers only name the cases
 * so the service and controller read clearly.
 */

export function userBadRequest(description: string): OaException {
  return OaErrorMapper.badRequest(description);
}

export function userNotFound(): OaException {
  return OaErrorMapper.notFound('User not found.');
}

export function userConflict(): OaException {
  return OaErrorMapper.conflict('A user with this username already exists.');
}
