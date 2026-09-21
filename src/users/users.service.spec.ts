import { ValidationError } from "../common/errors";
import { CreateUserDto } from "./dto/create-user.dto";
import { UsersService } from "./users.service";

function createAdmin(location = "http://kc/admin/realms/closed-cras/users/u-9") {
  return {
    post: jest.fn().mockResolvedValue({
      status: 201,
      body: undefined,
      headers: new Headers({ location }),
    }),
    put: jest.fn().mockResolvedValue({
      status: 204,
      body: undefined,
      headers: new Headers(),
    }),
    delete: jest.fn(),
  };
}

const newUser: CreateUserDto = {
  username: "ana.souza@pucrs.br",
  email: "ana.souza@pucrs.br",
  firstName: "Ana",
  lastName: "Souza",
  password: "senha-segura",
};

describe("UsersService", () => {
  describe("create", () => {
    it("sends the full representation and returns the id from Location", async () => {
      const admin = createAdmin();

      await expect(
        new UsersService(admin as never).create(newUser),
      ).resolves.toEqual({ id: "u-9" });
      expect(admin.post).toHaveBeenCalledWith("/users", {
        username: "ana.souza@pucrs.br",
        email: "ana.souza@pucrs.br",
        firstName: "Ana",
        lastName: "Souza",
        enabled: true,
        emailVerified: false,
        credentials: [
          { type: "password", value: "senha-segura", temporary: false },
        ],
      });
    });

    it("honours an explicit enabled=false", async () => {
      const admin = createAdmin();

      await new UsersService(admin as never).create({
        ...newUser,
        enabled: false,
      });

      expect(admin.post).toHaveBeenCalledWith(
        "/users",
        expect.objectContaining({ enabled: false }),
      );
    });

    it.each([
      ["no Location header", undefined],
      ["a Location with no id segment", "http://kc/admin/realms/closed-cras/users"],
    ])("fails loudly on %s", async (_case, location) => {
      const admin = createAdmin();
      admin.post.mockResolvedValue({
        status: 201,
        body: undefined,
        headers: new Headers(location ? { location } : {}),
      });

      await expect(
        new UsersService(admin as never).create(newUser),
      ).rejects.toThrow(/pode ter sido criado/);
    });
  });

  describe("replace", () => {
    it("writes every field and defaults enabled to true", async () => {
      const admin = createAdmin();

      await new UsersService(admin as never).replace("u-9", {
        username: "ana.souza@pucrs.br",
        email: "nova@pucrs.br",
        firstName: "Ana",
        lastName: "Souza",
      });

      expect(admin.put).toHaveBeenCalledWith("/users/u-9", {
        username: "ana.souza@pucrs.br",
        email: "nova@pucrs.br",
        firstName: "Ana",
        lastName: "Souza",
        enabled: true,
        emailVerified: false,
      });
    });
  });

  describe("patch", () => {
    it("sends only the fields that were provided", async () => {
      const admin = createAdmin();

      await new UsersService(admin as never).patch("u-9", {
        firstName: "Aninha",
      });

      expect(admin.put).toHaveBeenCalledTimes(1);
      expect(admin.put).toHaveBeenCalledWith("/users/u-9", {
        firstName: "Aninha",
      });
    });

    it("changes the password before touching the profile", async () => {
      const admin = createAdmin();

      await new UsersService(admin as never).patch("u-9", {
        firstName: "Aninha",
        password: "senha-nova",
      });

      // A política de senha do realm é a recusa mais provável: falhar antes de
      // gravar o perfil deixa o usuário intacto.
      expect(admin.put).toHaveBeenNthCalledWith(
        1,
        "/users/u-9/reset-password",
        { type: "password", value: "senha-nova", temporary: false },
      );
      expect(admin.put).toHaveBeenNthCalledWith(2, "/users/u-9", {
        firstName: "Aninha",
      });
    });

    it("leaves the profile untouched when reset-password fails", async () => {
      const admin = createAdmin();
      admin.put.mockRejectedValueOnce(new Error("password policy"));

      await expect(
        new UsersService(admin as never).patch("u-9", {
          firstName: "Aninha",
          password: "123456",
        }),
      ).rejects.toThrow("password policy");
      expect(admin.put).toHaveBeenCalledTimes(1);
    });

    it("drops explicit nulls instead of forwarding them to Keycloak", async () => {
      const admin = createAdmin();

      await new UsersService(admin as never).patch("u-9", {
        firstName: "Aninha",
        lastName: null as never,
      });

      expect(admin.put).toHaveBeenCalledWith("/users/u-9", {
        firstName: "Aninha",
      });
    });

    it("refuses a patch made only of nulls", async () => {
      const admin = createAdmin();

      await expect(
        new UsersService(admin as never).patch("u-9", {
          firstName: null as never,
        }),
      ).rejects.toBeInstanceOf(ValidationError);
      expect(admin.put).not.toHaveBeenCalled();
    });

    it("clears emailVerified when the email changes", async () => {
      const admin = createAdmin();

      await new UsersService(admin as never).patch("u-9", {
        email: "nova@pucrs.br",
      });

      expect(admin.put).toHaveBeenCalledWith("/users/u-9", {
        email: "nova@pucrs.br",
        emailVerified: false,
      });
    });

    it("skips the user body when only the password changes", async () => {
      const admin = createAdmin();

      await new UsersService(admin as never).patch("u-9", {
        password: "senha-nova",
      });

      expect(admin.put).toHaveBeenCalledTimes(1);
      expect(admin.put).toHaveBeenCalledWith(
        "/users/u-9/reset-password",
        expect.objectContaining({ value: "senha-nova" }),
      );
    });

    it("refuses an empty patch without touching Keycloak", async () => {
      const admin = createAdmin();

      await expect(
        new UsersService(admin as never).patch("u-9", {}),
      ).rejects.toBeInstanceOf(ValidationError);
      expect(admin.put).not.toHaveBeenCalled();
    });

    it("keeps enabled=false in the patch body", async () => {
      const admin = createAdmin();

      await new UsersService(admin as never).patch("u-9", { enabled: false });

      expect(admin.put).toHaveBeenCalledWith("/users/u-9", { enabled: false });
    });
  });

  describe("deactivate", () => {
    it("disables the user instead of deleting it", async () => {
      const admin = createAdmin();

      await new UsersService(admin as never).deactivate("u-9");

      expect(admin.put).toHaveBeenCalledWith("/users/u-9", { enabled: false });
      expect(admin.delete).not.toHaveBeenCalled();
    });

    it("escapes the id in the path", async () => {
      const admin = createAdmin();

      await new UsersService(admin as never).deactivate("u 9/../admin");

      expect(admin.put).toHaveBeenCalledWith(
        "/users/u%209%2F..%2Fadmin",
        expect.anything(),
      );
    });
  });
});
