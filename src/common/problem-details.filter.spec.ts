import { ArgumentsHost, BadRequestException } from "@nestjs/common";
import { KeycloakDependencyError } from "./errors";
import { ProblemDetailsFilter } from "./problem-details.filter";

describe("ProblemDetailsFilter", () => {
  function createHost() {
    const response = {
      status: jest.fn().mockReturnThis(),
      type: jest.fn().mockReturnThis(),
      send: jest.fn(),
    };
    const request = { url: "/v1/auth/login" };
    const host = {
      switchToHttp: () => ({
        getResponse: () => response,
        getRequest: () => request,
      }),
    } as unknown as ArgumentsHost;

    return { host, response };
  }

  it.each([
    [new KeycloakDependencyError("invalid_credentials", 401), 401, "OA-401"],
    [new KeycloakDependencyError("unavailable"), 503, "OA-503"],
    [new BadRequestException(), 400, "OA-400"],
    [new Error("unexpected"), 500, "OA-500"],
  ])("maps %s to the expected problem response", (exception, status, code) => {
    const { host, response } = createHost();

    new ProblemDetailsFilter().catch(exception, host);

    expect(response.status).toHaveBeenCalledWith(status);
    expect(response.type).toHaveBeenCalledWith("application/problem+json");
    expect(response.send).toHaveBeenCalledWith(
      expect.objectContaining({
        status,
        code,
        instance: "/v1/auth/login",
      }),
    );
  });
});
