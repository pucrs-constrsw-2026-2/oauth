import { UsersService } from "./users.service";
import { NotFoundException } from "@nestjs/common";
import { KeycloakDependencyError } from "../common/errors";

describe("UsersService", () => {
  it("maps Keycloak user fields to the public response contract", async () => {
    const keycloak = {
      listUsers: jest.fn().mockResolvedValue([
        {
          id: "1",
          username: "a@b.com",
          firstName: "Ada",
          lastName: "Lovelace",
          enabled: true,
        },
      ]),
    };
    const service = new UsersService(keycloak as never);

    await expect(service.list("token", false)).resolves.toEqual([
      {
        id: "1",
        username: "a@b.com",
        "first-name": "Ada",
        "last-name": "Lovelace",
        enabled: true,
      },
    ]);
    expect(keycloak.listUsers).toHaveBeenCalledWith("token", false);
  });

  it("uses empty strings when Keycloak omits names", async () => {
    const keycloak = {
      getUser: jest
        .fn()
        .mockResolvedValue({ id: "1", username: "a@b.com", enabled: false }),
    };
    const service = new UsersService(keycloak as never);

    await expect(service.get("token", "1")).resolves.toEqual({
      id: "1",
      username: "a@b.com",
      "first-name": "",
      "last-name": "",
      enabled: false,
    });
  });

  it("does not expose Keycloak service-account users", async () => {
    const keycloak = {
      listUsers: jest.fn().mockResolvedValue([
        { id: "1", username: "admin@pucrs.br", enabled: true },
        { id: "2", username: "service-account-oauth", enabled: true },
      ]),
    };
    const service = new UsersService(keycloak as never);

    await expect(service.list("token", true)).resolves.toHaveLength(1);
  });

  it("maps a missing user to NotFound and preserves other dependency errors", async () => {
    const missing = new UsersService({
      getUser: jest
        .fn()
        .mockRejectedValue(new KeycloakDependencyError("not_found", 404)),
    } as never);
    await expect(missing.get("token", "missing")).rejects.toBeInstanceOf(
      NotFoundException,
    );

    const forbidden = new KeycloakDependencyError("forbidden", 403);
    const denied = new UsersService({
      getUser: jest.fn().mockRejectedValue(forbidden),
    } as never);
    await expect(denied.get("token", "denied")).rejects.toBe(forbidden);
  });
});
