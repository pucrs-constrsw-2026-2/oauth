import { INestApplication, ValidationPipe } from "@nestjs/common";
import { Test } from "@nestjs/testing";
import request from "supertest";
import { AppModule } from "../src/app.module";
import { ErrorResponseFilter } from "../src/common/error-response.filter";

interface FakeRole {
  id: string;
  name: string;
  description?: string;
  clientRole?: boolean;
  attributes?: Record<string, string[]>;
}

const REALM_BASE = "/admin/realms/constrsw";

/**
 * In-memory Keycloak Admin API stand-in wired into global.fetch. Keeps roles and
 * user role-mappings so the full HTTP stack (routing -> pipe -> controller ->
 * service -> admin client -> error filter) can be exercised without Keycloak.
 */
function createKeycloakFake() {
  const roles = new Map<string, FakeRole>();
  const assignments = new Map<string, Set<string>>();
  let seq = 0;

  function response(status: number, body?: unknown): Response {
    const text = body === undefined ? "" : JSON.stringify(body);
    return {
      ok: status >= 200 && status < 300,
      status,
      headers: new Headers(),
      json: async () => body ?? {},
      text: async () => text,
    } as unknown as Response;
  }

  const impl = async (
    input: URL | RequestInfo,
    init?: RequestInit,
  ): Promise<Response> => {
    const url = new URL(String(input));
    const path = url.pathname;
    const method = (init?.method ?? "GET").toUpperCase();

    // Token requests carry a form-encoded body; return before JSON-parsing.
    if (path.endsWith("/protocol/openid-connect/token")) {
      return response(200, {
        token_type: "Bearer",
        access_token: "test-token",
        expires_in: 300,
      });
    }

    // Every remaining (Admin API) call uses a JSON body when it has one.
    const body = init?.body ? JSON.parse(String(init.body)) : undefined;

    if (path === `${REALM_BASE}/roles` && method === "POST") {
      if ([...roles.values()].some((r) => r.name === body.name)) {
        return response(409);
      }
      const id = `role-${++seq}`;
      roles.set(id, {
        id,
        name: body.name,
        description: body.description,
        attributes: body.attributes,
      });
      return response(201);
    }

    if (path === `${REALM_BASE}/roles` && method === "GET") {
      // Mirror Keycloak: the list omits `attributes` unless briefRepresentation=false.
      const full = url.searchParams.get("briefRepresentation") === "false";
      const list = [...roles.values()].map((r) =>
        full ? r : { ...r, attributes: undefined },
      );
      return response(200, list);
    }

    const holders = path.match(
      /^\/admin\/realms\/constrsw\/roles\/([^/]+)\/users$/,
    );
    if (holders && method === "GET") {
      const role = [...roles.values()].find(
        (r) => r.name === decodeURIComponent(holders[1]),
      );
      if (!role) return response(404);
      const first = Number(url.searchParams.get("first") ?? 0);
      const max = Number(url.searchParams.get("max") ?? 100);
      const users = [...assignments.entries()]
        .filter(([, set]) => set.has(role.id))
        .map(([id]) => ({ id }));
      return response(200, users.slice(first, first + max));
    }

    const byName = path.match(/^\/admin\/realms\/constrsw\/roles\/(.+)$/);
    if (byName && method === "GET") {
      const role = [...roles.values()].find(
        (r) => r.name === decodeURIComponent(byName[1]),
      );
      return role ? response(200, role) : response(404);
    }

    const byId = path.match(/^\/admin\/realms\/constrsw\/roles-by-id\/(.+)$/);
    if (byId) {
      const id = decodeURIComponent(byId[1]);
      if (method === "GET") {
        const role = roles.get(id);
        return role ? response(200, role) : response(404);
      }
      if (method === "PUT") {
        const current = roles.get(id);
        if (!current) return response(404);
        const taken = [...roles.values()].some(
          (r) => r.id !== id && r.name === body.name,
        );
        if (taken) return response(409);
        roles.set(id, {
          ...current,
          name: body.name ?? current.name,
          description: body.description,
          attributes: body.attributes ?? current.attributes,
        });
        return response(204);
      }
    }

    const mapping = path.match(
      /^\/admin\/realms\/constrsw\/users\/([^/]+)\/role-mappings\/realm$/,
    );
    if (mapping) {
      const userId = mapping[1];
      const set = assignments.get(userId) ?? new Set<string>();
      const refs: Array<{ id: string }> = body ?? [];
      if (method === "POST") {
        refs.forEach((r) => set.add(r.id));
        assignments.set(userId, set);
        return response(204);
      }
      if (method === "DELETE") {
        refs.forEach((r) => set.delete(r.id));
        assignments.set(userId, set);
        return response(204);
      }
    }

    return response(404);
  };

  return { impl, roles, assignments };
}

describe("Roles (e2e)", () => {
  let app: INestApplication;
  let keycloak: ReturnType<typeof createKeycloakFake>;

  beforeEach(async () => {
    keycloak = createKeycloakFake();
    jest
      .spyOn(global, "fetch")
      .mockImplementation(keycloak.impl as unknown as typeof fetch);

    const moduleRef = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleRef.createNestApplication();
    app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
    app.useGlobalFilters(new ErrorResponseFilter());
    await app.init();
  });

  afterEach(async () => {
    await app.close();
    jest.restoreAllMocks();
  });

  const server = () => app.getHttpServer();

  it("runs the full CRUD lifecycle ending in a logical delete", async () => {
    const created = await request(server())
      .post("/v1/roles")
      .send({ name: "professor", description: "Docente" })
      .expect(201);
    const id = created.body.id as string;
    expect(created.body).toMatchObject({ name: "professor" });
    expect(id).toBeDefined();

    await request(server())
      .get("/v1/roles")
      .expect(200)
      .expect((res) => expect(res.body).toHaveLength(1));

    await request(server()).get(`/v1/roles/${id}`).expect(200);

    await request(server())
      .put(`/v1/roles/${id}`)
      .send({ name: "professor-x" })
      .expect(200)
      .expect((res) => expect(res.body.name).toBe("professor-x"));

    await request(server())
      .patch(`/v1/roles/${id}`)
      .send({ description: "atualizado" })
      .expect(200);

    await request(server()).delete(`/v1/roles/${id}`).expect(204);

    // After logical delete the role is hidden from every read path.
    await request(server()).get(`/v1/roles/${id}`).expect(404);
    await request(server())
      .get("/v1/roles")
      .expect(200)
      .expect((res) => expect(res.body).toHaveLength(0));
  });

  it("assigns and unassigns a role to a user", async () => {
    const created = await request(server())
      .post("/v1/roles")
      .send({ name: "aluno" })
      .expect(201);
    const id = created.body.id as string;

    await request(server()).post(`/v1/roles/${id}/users/user-1`).expect(204);
    expect(keycloak.assignments.get("user-1")?.has(id)).toBe(true);

    await request(server()).delete(`/v1/roles/${id}/users/user-1`).expect(204);
    expect(keycloak.assignments.get("user-1")?.has(id)).toBe(false);
  });

  it("maps upstream conflicts and missing roles to the error envelope", async () => {
    await request(server()).post("/v1/roles").send({ name: "dup" }).expect(201);

    await request(server())
      .post("/v1/roles")
      .send({ name: "dup" })
      .expect(409)
      .expect((res) => expect(res.body.error_code).toBe("OA-409"));

    await request(server())
      .get("/v1/roles/inexistente")
      .expect(404)
      .expect((res) => expect(res.body.error_code).toBe("OA-404"));
  });

  it("strips a deleted role from its holders and still allows unassigning it", async () => {
    const created = await request(server())
      .post("/v1/roles")
      .send({ name: "monitor" })
      .expect(201);
    const id = created.body.id as string;
    await request(server()).post(`/v1/roles/${id}/users/user-1`).expect(204);
    await request(server()).post(`/v1/roles/${id}/users/user-2`).expect(204);

    await request(server()).delete(`/v1/roles/${id}`).expect(204);

    expect(keycloak.assignments.get("user-1")?.has(id)).toBe(false);
    expect(keycloak.assignments.get("user-2")?.has(id)).toBe(false);
    expect(keycloak.roles.get(id)?.attributes).toEqual({ deleted: ["true"] });
    await request(server()).delete(`/v1/roles/${id}/users/user-1`).expect(204);
  });

  it("explains a name conflict with a logically deleted role", async () => {
    const created = await request(server())
      .post("/v1/roles")
      .send({ name: "antigo" })
      .expect(201);
    await request(server()).delete(`/v1/roles/${created.body.id}`).expect(204);

    await request(server())
      .post("/v1/roles")
      .send({ name: "antigo" })
      .expect(409)
      .expect((res) =>
        expect(res.body.error_description).toMatch(/papel excluído/),
      );
  });

  it("words a rename conflict as a role conflict", async () => {
    await request(server()).post("/v1/roles").send({ name: "a" }).expect(201);
    const b = await request(server())
      .post("/v1/roles")
      .send({ name: "b" })
      .expect(201);

    for (const method of ["put", "patch"] as const) {
      await request(server())
        [method](`/v1/roles/${b.body.id}`)
        .send({ name: "a" })
        .expect(409)
        .expect((res) =>
          expect(res.body.error_description).toBe(
            "Já existe um papel com este nome.",
          ),
        );
    }
  });

  it("does not expose client roles through the by-id endpoints", async () => {
    keycloak.roles.set("client-1", {
      id: "client-1",
      name: "manage-users",
      clientRole: true,
    });

    await request(server()).get("/v1/roles/client-1").expect(404);
    await request(server())
      .patch("/v1/roles/client-1")
      .send({ description: "x" })
      .expect(404);
    await request(server()).delete("/v1/roles/client-1").expect(404);
    await request(server())
      .post("/v1/roles/client-1/users/user-1")
      .expect(404)
      .expect((res) =>
        expect(res.body.error_description).toBe("Papel não encontrado no realm."),
      );
  });

  it("rejects invalid payloads with 400", async () => {
    await request(server())
      .post("/v1/roles")
      .send({ description: "sem nome" })
      .expect(400)
      .expect((res) => expect(res.body.error_code).toBe("OA-400"));
  });
});
