import { HttpException, HttpStatus } from '@nestjs/common';

import { OA_ERROR_CODE, OA_ERROR_SOURCE, OaError } from './oa-error';

/**
 * Thrown by route handlers that already know the OA shape they want. The
 * global filter (`OaExceptionFilter`) formats it; nothing else needs to know
 * about `error_code` / `error_stack` field names.
 */
export class OaException extends HttpException {
  constructor(
    status: number,
    private readonly errorCode: string,
    private readonly errorDescription: string,
    private readonly errorStack: Record<string, unknown>[] = [],
  ) {
    super(errorDescription, status);
  }

  toEnvelope(): OaError {
    return {
      error_code: this.errorCode,
      error_description: this.errorDescription,
      error_source: OA_ERROR_SOURCE,
      error_stack:
        this.errorStack.length > 0
          ? this.errorStack
          : [{ error_code: this.errorCode, message: this.errorDescription }],
    };
  }
}

/** Maps an HTTP status Nest would use to the local `OA-xxx` code family. */
function localCodeForStatus(status: number): string {
  switch (status) {
    case HttpStatus.BAD_REQUEST:
      return OA_ERROR_CODE.BAD_REQUEST;
    case HttpStatus.UNAUTHORIZED:
      return OA_ERROR_CODE.UNAUTHORIZED;
    case HttpStatus.FORBIDDEN:
      return OA_ERROR_CODE.FORBIDDEN;
    case HttpStatus.NOT_FOUND:
      return OA_ERROR_CODE.NOT_FOUND;
    case HttpStatus.CONFLICT:
      return OA_ERROR_CODE.CONFLICT;
    default:
      return OA_ERROR_CODE.INTERNAL;
  }
}

/** Response shape the global exception filter writes to the wire. */
export interface OaMappedResponse {
  readonly status: number;
  readonly body: OaError;
}

/**
 * Central place that turns any thrown value into `{ status, body }`. Route
 * modules should prefer throwing via the factory helpers below rather than
 * calling this directly — it exists for the global filter.
 */
export const OaErrorMapper = {
  /** Local (non-Keycloak) failure: bad structure, missing auth, etc. */
  badRequest(description: string, cause?: Record<string, unknown>): OaException {
    return new OaException(
      HttpStatus.BAD_REQUEST,
      OA_ERROR_CODE.BAD_REQUEST,
      description,
      cause ? [cause] : [],
    );
  },

  unauthorized(description: string, cause?: Record<string, unknown>): OaException {
    return new OaException(
      HttpStatus.UNAUTHORIZED,
      OA_ERROR_CODE.UNAUTHORIZED,
      description,
      cause ? [cause] : [],
    );
  },

  forbidden(description: string, cause?: Record<string, unknown>): OaException {
    return new OaException(
      HttpStatus.FORBIDDEN,
      OA_ERROR_CODE.FORBIDDEN,
      description,
      cause ? [cause] : [],
    );
  },

  notFound(description: string, cause?: Record<string, unknown>): OaException {
    return new OaException(
      HttpStatus.NOT_FOUND,
      OA_ERROR_CODE.NOT_FOUND,
      description,
      cause ? [cause] : [],
    );
  },

  conflict(description: string, cause?: Record<string, unknown>): OaException {
    return new OaException(
      HttpStatus.CONFLICT,
      OA_ERROR_CODE.CONFLICT,
      description,
      cause ? [cause] : [],
    );
  },

  /**
   * Failure at an arbitrary status with no upstream body to relay — a gateway
   * or availability problem, for example. Statuses outside the local family
   * keep their own number (`OA-502`) instead of collapsing into `OA-500`.
   */
  fromStatus(
    status: number,
    description: string,
    cause?: Record<string, unknown>,
  ): OaException {
    const known = localCodeForStatus(status);
    const code =
      known === OA_ERROR_CODE.INTERNAL && status !== HttpStatus.INTERNAL_SERVER_ERROR
        ? `OA-${status}`
        : known;

    return new OaException(status, code, description, cause ? [cause] : []);
  },

  /**
   * Relays a Keycloak error payload (`{ error, error_description }` from the
   * token endpoint, or similar) into the envelope. `error_code` becomes
   * Keycloak's `error` field so grader tooling sees the upstream code
   * verbatim, per AC #2 of Story 3.1.
   */
  fromKeycloak(
    status: number,
    description: string,
    keycloakBody: unknown,
  ): OaException {
    const keycloakError = extractString(keycloakBody, 'error');
    const code = keycloakError ?? localCodeForStatus(status);
    const stack: Record<string, unknown>[] = [
      {
        source: 'keycloak',
        ...(isRecord(keycloakBody) ? keycloakBody : { raw: keycloakBody }),
      },
    ];
    return new OaException(status, code, description, stack);
  },

  /** Converts any thrown value into a wire-ready `{ status, body }` pair. */
  toResponse(exception: unknown): OaMappedResponse {
    if (exception instanceof OaException) {
      return { status: exception.getStatus(), body: exception.toEnvelope() };
    }

    if (exception instanceof HttpException) {
      const status = exception.getStatus();
      const response = exception.getResponse();

      // A payload that is already an OA envelope passes through untouched.
      // Without this, Nest replaces the description with the exception class
      // name, because it looks for `message` and the envelope has none.
      if (isOaEnvelope(response)) {
        return { status, body: response };
      }

      const description =
        typeof response === 'string'
          ? response
          : ((response as { message?: unknown })?.message ?? exception.message);

      return {
        status,
        body: {
          error_code: localCodeForStatus(status),
          error_description: Array.isArray(description)
            ? description.join('; ')
            : String(description),
          error_source: OA_ERROR_SOURCE,
          error_stack: [{ statusCode: status, message: description }],
        },
      };
    }

    const message = exception instanceof Error ? exception.message : 'Unexpected error';
    return {
      status: HttpStatus.INTERNAL_SERVER_ERROR,
      body: {
        error_code: OA_ERROR_CODE.INTERNAL,
        error_description: 'Unexpected internal error.',
        error_source: OA_ERROR_SOURCE,
        error_stack: [
          {
            message,
            name: exception instanceof Error ? exception.name : 'UnknownError',
          },
        ],
      },
    };
  },
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function extractString(value: unknown, key: string): string | undefined {
  if (!isRecord(value)) {
    return undefined;
  }
  const field = value[key];
  return typeof field === 'string' && field.length > 0 ? field : undefined;
}

function isOaEnvelope(value: unknown): value is OaError {
  if (!isRecord(value)) {
    return false;
  }
  return (
    typeof value.error_code === 'string' &&
    typeof value.error_description === 'string' &&
    typeof value.error_source === 'string' &&
    Array.isArray(value.error_stack)
  );
}
