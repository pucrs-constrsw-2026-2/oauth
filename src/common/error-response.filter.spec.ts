import { ArgumentsHost, BadRequestException, HttpException, HttpStatus } from "@nestjs/common";
import { ErrorResponseFilter } from "./error-response.filter";
import {
  ConflictError,
  KeycloakDependencyError,
  KeycloakError,
  NotFoundError,
  ValidationError,
} from "./errors";

describe("ErrorResponseFilter", () => {
  function createHost() {
    const response = {
      status: jest.fn().mockReturnThis(),
      type: jest.fn().mockReturnThis(),
      send: jest.fn(),
    };
    const host = {
      switchToHttp: () => ({
        getResponse: () => response,
        getRequest: () => ({ url: "/v1/users" }),
      }),
    } as unknown as ArgumentsHost;

    return { host, response };
  }

  function capture(exception: unknown) {
    const { host, response } = createHost();
    new ErrorResponseFilter().catch(exception, host);
    return {
      status: response.status.mock.calls[0][0] as number,
      body: response.send.mock.calls[0][0] as Record<string, unknown>,
    };
  }

  it.each([
    [new ValidationError("inválido"), 400, "OA-400", "validation"],
    [new NotFoundError("sumiu"), 404, "OA-404", "keycloak"],
    [new ConflictError("duplicado"), 409, "OA-409", "keycloak"],
    [new KeycloakError("inesperado"), 502, "OA-502", "keycloak"],
    [new KeycloakDependencyError("unavailable"), 503, "OA-503", "keycloak"],
    [
      new KeycloakDependencyError("invalid_credentials", 401),
      401,
      "OA-401",
      "keycloak",
    ],
  ])("maps %s to its status and code", (exception, status, code, source) => {
    const result = capture(exception);

    expect(result.status).toBe(status);
    expect(result.body).toEqual({
      error_code: code,
      error_description: (exception as Error).message,
      error_source: source,
      error_stack: [
        { source, code, description: (exception as Error).message },
      ],
    });
  });

  it("always answers with exactly the four agreed keys", () => {
    const result = capture(new ConflictError("duplicado"));

    expect(Object.keys(result.body).sort()).toEqual([
      "error_code",
      "error_description",
      "error_source",
      "error_stack",
    ]);
  });

  it("turns each class-validator message into a stack entry", () => {
    const result = capture(
      new BadRequestException({
        statusCode: 400,
        message: ["email deve ser válido", "password é obrigatório"],
      }),
    );

    expect(result.status).toBe(400);
    expect(result.body.error_source).toBe("validation");
    expect(result.body.error_stack).toEqual([
      {
        source: "validation",
        code: "OA-400",
        description: "email deve ser válido",
      },
      {
        source: "validation",
        code: "OA-400",
        description: "password é obrigatório",
      },
    ]);
  });

  it("maps a plain HttpException without validation details", () => {
    const result = capture(
      new HttpException("Não autorizado", HttpStatus.UNAUTHORIZED),
    );

    expect(result.status).toBe(401);
    expect(result.body).toEqual({
      error_code: "OA-401",
      error_description: "Não autorizado",
      error_source: "oauth",
      error_stack: [],
    });
  });

  it("hides the cause of an unexpected error in production", () => {
    const previous = process.env.NODE_ENV;
    process.env.NODE_ENV = "production";
    try {
      const result = capture(new Error("segredo no stack"));

      expect(result.status).toBe(500);
      expect(result.body.error_description).toBe(
        "Erro interno no serviço de identidade.",
      );
      expect(result.body.error_stack).toEqual([]);
    } finally {
      process.env.NODE_ENV = previous;
    }
  });

  it("exposes the cause of an unexpected error outside production", () => {
    const result = capture(new Error("boom"));

    expect(result.status).toBe(500);
    expect(result.body.error_stack).toEqual([
      { source: "runtime", code: "OA-500", description: "boom" },
    ]);
  });
});
