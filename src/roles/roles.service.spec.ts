import { NotFoundException } from "@nestjs/common";
import { KeycloakDependencyError } from "../common/errors";
import { RolesService } from "./roles.service";

function createService(overrides: Record<string, jest.Mock> = {}) {
  const admin = {
    createRole: jest.fn().mockResolvedValue(undefined),
    getRoleByName: jest.fn(),
    listRoles: jest.fn(),
    getRoleById: jest.fn(),
    updateRoleById: jest.fn().mockResolvedValue(undefined),
    assignRealmRole: jest.fn().mockResolvedValue(undefined),
    removeRealmRole: jest.fn().mockResolvedValue(undefined),
    ...overrides,
  };
  return { service: new RolesService(admin as never), admin };
}

describe("RolesService", () => {
  it("creates a role and re-reads it by name to expose the id", async () => {
    const { service, admin } = createService();
    admin.getRoleByName.mockResolvedValue({
      id: "r1",
      name: "professor",
      description: "Docente",
    });

    await expect(
      service.create({ name: "professor", description: "Docente" }),
    ).resolves.toEqual({ id: "r1", name: "professor", description: "Docente" });
    expect(admin.createRole).toHaveBeenCalledWith({
      name: "professor",
      description: "Docente",
    });
    expect(admin.getRoleByName).toHaveBeenCalledWith("professor");
  });

  it("lists only roles that are not logically deleted", async () => {
    const { service, admin } = createService();
    admin.listRoles.mockResolvedValue([
      { id: "r1", name: "professor" },
      { id: "r2", name: "antigo", attributes: { deleted: ["true"] } },
    ]);

    await expect(service.findAll()).resolves.toEqual([
      { id: "r1", name: "professor" },
    ]);
  });

  it("returns a single active role", async () => {
    const { service, admin } = createService();
    admin.getRoleById.mockResolvedValue({ id: "r1", name: "professor" });

    await expect(service.findOne("r1")).resolves.toEqual({
      id: "r1",
      name: "professor",
    });
  });

  it("treats a logically deleted role as not found", async () => {
    const { service, admin } = createService();
    admin.getRoleById.mockResolvedValue({
      id: "r1",
      name: "antigo",
      attributes: { deleted: ["true"] },
    });

    await expect(service.findOne("r1")).rejects.toBeInstanceOf(
      NotFoundException,
    );
  });

  it("maps an upstream 404 to a NotFound exception", async () => {
    const { service, admin } = createService();
    admin.getRoleById.mockRejectedValue(
      new KeycloakDependencyError("not_found", 404),
    );

    await expect(service.findOne("missing")).rejects.toBeInstanceOf(
      NotFoundException,
    );
  });

  it("logically deletes by setting the deleted attribute", async () => {
    const { service, admin } = createService();
    admin.getRoleById.mockResolvedValue({ id: "r1", name: "professor" });

    await service.remove("r1");

    expect(admin.updateRoleById).toHaveBeenCalledWith(
      "r1",
      expect.objectContaining({ attributes: { deleted: ["true"] } }),
    );
  });

  it("assigns a role to a user using id and name", async () => {
    const { service, admin } = createService();
    admin.getRoleById.mockResolvedValue({ id: "r1", name: "professor" });

    await service.assignToUser("r1", "user-1");

    expect(admin.assignRealmRole).toHaveBeenCalledWith("user-1", {
      id: "r1",
      name: "professor",
    });
  });

  it("removes a role from a user using id and name", async () => {
    const { service, admin } = createService();
    admin.getRoleById.mockResolvedValue({ id: "r1", name: "professor" });

    await service.removeFromUser("r1", "user-1");

    expect(admin.removeRealmRole).toHaveBeenCalledWith("user-1", {
      id: "r1",
      name: "professor",
    });
  });

  it("fully updates a role and returns the reloaded value", async () => {
    const { service, admin } = createService();
    admin.getRoleById
      .mockResolvedValueOnce({ id: "r1", name: "antigo" })
      .mockResolvedValueOnce({ id: "r1", name: "novo", description: "d" });

    await expect(
      service.update("r1", { name: "novo", description: "d" }),
    ).resolves.toEqual({ id: "r1", name: "novo", description: "d" });
    expect(admin.updateRoleById).toHaveBeenCalledWith(
      "r1",
      expect.objectContaining({ name: "novo", description: "d" }),
    );
  });

  it("patches only the provided fields", async () => {
    const { service, admin } = createService();
    admin.getRoleById
      .mockResolvedValueOnce({ id: "r1", name: "antigo", description: "manter" })
      .mockResolvedValueOnce({
        id: "r1",
        name: "antigo",
        description: "alterado",
      });

    await service.patch("r1", { description: "alterado" });

    expect(admin.updateRoleById).toHaveBeenCalledWith(
      "r1",
      expect.objectContaining({ name: "antigo", description: "alterado" }),
    );
  });
});
