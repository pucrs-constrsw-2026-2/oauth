import { BadRequestException, UnauthorizedException } from "@nestjs/common";
import { UsersController } from "./users.controller";

describe("UsersController", () => {
  it("passes the bearer token and enabled filter to the service", async () => {
    const users = { list: jest.fn().mockResolvedValue([]) };
    const controller = new UsersController(users as never);

    await controller.list("Bearer access-token", true);

    expect(users.list).toHaveBeenCalledWith("access-token", true);
  });

  it("gets a user by id with the bearer token", async () => {
    const users = { get: jest.fn().mockResolvedValue({ id: "user-1" }) };
    const controller = new UsersController(users as never);

    await controller.get("bearer access-token", "user-1");

    expect(users.get).toHaveBeenCalledWith("access-token", "user-1");
  });

  it.each([undefined, "Basic credentials", "Bearer"])(
    "rejects malformed authorization header: %s",
    (authorization) => {
      const controller = new UsersController({ list: jest.fn() } as never);

      expect(() => controller.list(authorization)).toThrow(
        UnauthorizedException,
      );
    },
  );
});

it("rejects a missing user id", () => {
  const controller = new UsersController({ get: jest.fn() } as never);

  expect(() => controller.get("Bearer token", undefined)).toThrow(
    BadRequestException,
  );
});
