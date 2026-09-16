import { UsersController } from "./users.controller";

describe("UsersController", () => {
  function createController() {
    const users = {
      create: jest.fn().mockResolvedValue({ id: "u-9" }),
      replace: jest.fn().mockResolvedValue(undefined),
      patch: jest.fn().mockResolvedValue(undefined),
      deactivate: jest.fn().mockResolvedValue(undefined),
    };

    return { users, controller: new UsersController(users as never) };
  }

  const body = {
    username: "ana.souza@pucrs.br",
    email: "ana.souza@pucrs.br",
    firstName: "Ana",
    lastName: "Souza",
    password: "senha-segura",
  };

  it("returns the created id", async () => {
    const { users, controller } = createController();

    await expect(controller.create(body)).resolves.toEqual({ id: "u-9" });
    expect(users.create).toHaveBeenCalledWith(body);
  });

  it("delegates a full replace", async () => {
    const { users, controller } = createController();
    const { password: _password, ...replacement } = body;

    await expect(controller.replace("u-9", replacement)).resolves.toBeUndefined();
    expect(users.replace).toHaveBeenCalledWith("u-9", replacement);
  });

  it("delegates a partial change", async () => {
    const { users, controller } = createController();

    await expect(
      controller.patch("u-9", { firstName: "Aninha" }),
    ).resolves.toBeUndefined();
    expect(users.patch).toHaveBeenCalledWith("u-9", { firstName: "Aninha" });
  });

  it("maps DELETE to the logical deactivation", async () => {
    const { users, controller } = createController();

    await expect(controller.remove("u-9")).resolves.toBeUndefined();
    expect(users.deactivate).toHaveBeenCalledWith("u-9");
  });
});
