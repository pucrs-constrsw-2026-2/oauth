import { ConfigService } from "@nestjs/config";
import { KeycloakDependencyError } from "../common/errors";
import { KeycloakAdminClient } from "./keycloak-admin.client";

describe("KeycloakAdminClient", () => {
  const configValues = {
    KEYCLOAK_URL: "http://keycloak:8080",
    KEYCLOAK_REALM: "constrsw",
    KEYCLOAK_TIMEOUT_MS: 5000,
    KEYCLOAK_ADMIN_REALM: "master",
    KEYCLOAK_ADMIN_CLIENT_ID: "admin-cli",
    KEYCLOAK_ADMIN: "admin",
    KEYCLOAK_ADMIN_PASSWORD: "secret",
  };

  function createClient() {
    const config = {
      getOrThrow: jest.fn(
        (key: string) => configValues[key as keyof typeof configValues],
      ),
    };
    return new KeycloakAdminClient(config as unknown as ConfigService);
  }

  function tokenResponse() {
    return {
      ok: true,
      status: 200,
      json: jest.fn().mockResolvedValue({
        token_type: "Bearer",
        access_token: "admin-token",
        expires_in: 60,
      }),
      text: jest.fn().mockResolvedValue(""),
    } as unknown as Response;
  }

  function jsonResponse(body: unknown, ok = true, status = 200) {
    return {
      ok,
      status,
      json: jest.fn().mockResolvedValue(body ?? {}),
      text: jest
        .fn()
        .mockResolvedValue(body === undefined ? "" : JSON.stringify(body)),
    } as unknown as Response;
  }

  beforeEach(() => {
    jest.restoreAllMocks();
  });

  it("obtains an admin token then lists roles with a bearer header", async () => {
    const fetchMock = jest
      .spyOn(global, "fetch")
      .mockResolvedValueOnce(tokenResponse())
      .mockResolvedValueOnce(jsonResponse([{ id: "r1", name: "professor" }]));

    await expect(createClient().listRoles()).resolves.toEqual([
      { id: "r1", name: "professor" },
    ]);

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      new URL("http://keycloak:8080/realms/master/protocol/openid-connect/token"),
      expect.objectContaining({ method: "POST" }),
    );
    const [tokenUrl, tokenOptions] = fetchMock.mock.calls[0];
    expect(String((tokenOptions?.body as URLSearchParams).toString())).toContain(
      "client_id=admin-cli",
    );
    expect(String(tokenUrl)).toContain("/realms/master/");

    const [rolesUrl, rolesOptions] = fetchMock.mock.calls[1];
    // Must request the full representation so role attributes are returned.
    expect(String(rolesUrl)).toBe(
      "http://keycloak:8080/admin/realms/constrsw/roles?briefRepresentation=false",
    );
    expect(
      (rolesOptions?.headers as Record<string, string>).authorization,
    ).toBe("Bearer admin-token");
  });

  it("caches the admin token across calls", async () => {
    const fetchMock = jest
      .spyOn(global, "fetch")
      .mockResolvedValueOnce(tokenResponse())
      .mockResolvedValue(jsonResponse([]));
    const client = createClient();

    await client.listRoles();
    await client.listRoles();

    const tokenCalls = fetchMock.mock.calls.filter(([url]) =>
      String(url).includes("/protocol/openid-connect/token"),
    );
    expect(tokenCalls).toHaveLength(1);
  });

  it("posts the realm role-mapping when assigning a role to a user", async () => {
    const fetchMock = jest
      .spyOn(global, "fetch")
      .mockResolvedValueOnce(tokenResponse())
      .mockResolvedValueOnce(jsonResponse(undefined, true, 204));

    await createClient().assignRealmRole("user-1", {
      id: "r1",
      name: "professor",
    });

    const [url, options] = fetchMock.mock.calls[1];
    expect(String(url)).toBe(
      "http://keycloak:8080/admin/realms/constrsw/users/user-1/role-mappings/realm",
    );
    expect(options?.method).toBe("POST");
    expect(options?.body).toBe(
      JSON.stringify([{ id: "r1", name: "professor" }]),
    );
  });

  it("maps a 404 from Keycloak to a not_found dependency error", async () => {
    jest
      .spyOn(global, "fetch")
      .mockResolvedValueOnce(tokenResponse())
      .mockResolvedValueOnce(jsonResponse(undefined, false, 404));

    await expect(createClient().getRoleById("missing")).rejects.toEqual(
      new KeycloakDependencyError("not_found", 404),
    );
  });

  it("maps a 409 from Keycloak to a conflict dependency error", async () => {
    jest
      .spyOn(global, "fetch")
      .mockResolvedValueOnce(tokenResponse())
      .mockResolvedValueOnce(jsonResponse(undefined, false, 409));

    await expect(
      createClient().createRole({ name: "duplicate" }),
    ).rejects.toEqual(new KeycloakDependencyError("conflict", 409));
  });
});
