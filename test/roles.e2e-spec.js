"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const common_1 = require("@nestjs/common");
const testing_1 = require("@nestjs/testing");
const supertest_1 = __importDefault(require("supertest"));
const app_module_1 = require("../src/app.module");
const problem_details_filter_1 = require("../src/common/problem-details.filter");
const REALM_BASE = "/admin/realms/constrsw";
function createKeycloakFake() {
    const roles = new Map();
    const assignments = new Map();
    let seq = 0;
    function response(status, body) {
        const text = body === undefined ? "" : JSON.stringify(body);
        return {
            ok: status >= 200 && status < 300,
            status,
            json: async () => body ?? {},
            text: async () => text,
        };
    }
    const impl = async (input, init) => {
        const url = new URL(String(input));
        const path = url.pathname;
        const method = (init?.method ?? "GET").toUpperCase();
        const body = init?.body ? JSON.parse(String(init.body)) : undefined;
        if (path.endsWith("/protocol/openid-connect/token")) {
            return response(200, {
                token_type: "Bearer",
                access_token: "test-token",
                expires_in: 300,
            });
        }
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
            return response(200, [...roles.values()]);
        }
        const byName = path.match(/^\/admin\/realms\/constrsw\/roles\/(.+)$/);
        if (byName && method === "GET") {
            const role = [...roles.values()].find((r) => r.name === decodeURIComponent(byName[1]));
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
                if (!current)
                    return response(404);
                roles.set(id, {
                    ...current,
                    name: body.name ?? current.name,
                    description: body.description,
                    attributes: body.attributes ?? current.attributes,
                });
                return response(204);
            }
        }
        const mapping = path.match(/^\/admin\/realms\/constrsw\/users\/([^/]+)\/role-mappings\/realm$/);
        if (mapping) {
            const userId = mapping[1];
            const set = assignments.get(userId) ?? new Set();
            const refs = body ?? [];
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
    let app;
    let keycloak;
    beforeEach(async () => {
        keycloak = createKeycloakFake();
        jest
            .spyOn(global, "fetch")
            .mockImplementation(keycloak.impl);
        const moduleRef = await testing_1.Test.createTestingModule({
            imports: [app_module_1.AppModule],
        }).compile();
        app = moduleRef.createNestApplication();
        app.useGlobalPipes(new common_1.ValidationPipe({ whitelist: true, transform: true }));
        app.useGlobalFilters(new problem_details_filter_1.ProblemDetailsFilter());
        await app.init();
    });
    afterEach(async () => {
        await app.close();
        jest.restoreAllMocks();
    });
    const server = () => app.getHttpServer();
    it("runs the full CRUD lifecycle ending in a logical delete", async () => {
        const created = await (0, supertest_1.default)(server())
            .post("/v1/roles")
            .send({ name: "professor", description: "Docente" })
            .expect(201);
        const id = created.body.id;
        expect(created.body).toMatchObject({ name: "professor" });
        expect(id).toBeDefined();
        await (0, supertest_1.default)(server())
            .get("/v1/roles")
            .expect(200)
            .expect((res) => expect(res.body).toHaveLength(1));
        await (0, supertest_1.default)(server()).get(`/v1/roles/${id}`).expect(200);
        await (0, supertest_1.default)(server())
            .put(`/v1/roles/${id}`)
            .send({ name: "professor-x" })
            .expect(200)
            .expect((res) => expect(res.body.name).toBe("professor-x"));
        await (0, supertest_1.default)(server())
            .patch(`/v1/roles/${id}`)
            .send({ description: "atualizado" })
            .expect(200);
        await (0, supertest_1.default)(server()).delete(`/v1/roles/${id}`).expect(204);
        await (0, supertest_1.default)(server()).get(`/v1/roles/${id}`).expect(404);
        await (0, supertest_1.default)(server())
            .get("/v1/roles")
            .expect(200)
            .expect((res) => expect(res.body).toHaveLength(0));
    });
    it("assigns and unassigns a role to a user", async () => {
        const created = await (0, supertest_1.default)(server())
            .post("/v1/roles")
            .send({ name: "aluno" })
            .expect(201);
        const id = created.body.id;
        await (0, supertest_1.default)(server()).post(`/v1/roles/${id}/users/user-1`).expect(204);
        expect(keycloak.assignments.get("user-1")?.has(id)).toBe(true);
        await (0, supertest_1.default)(server()).delete(`/v1/roles/${id}/users/user-1`).expect(204);
        expect(keycloak.assignments.get("user-1")?.has(id)).toBe(false);
    });
    it("maps upstream conflicts and missing roles to problem responses", async () => {
        await (0, supertest_1.default)(server()).post("/v1/roles").send({ name: "dup" }).expect(201);
        await (0, supertest_1.default)(server())
            .post("/v1/roles")
            .send({ name: "dup" })
            .expect(409)
            .expect((res) => expect(res.body.code).toBe("OA-409"));
        await (0, supertest_1.default)(server())
            .get("/v1/roles/inexistente")
            .expect(404)
            .expect((res) => expect(res.body.code).toBe("OA-404"));
    });
    it("rejects invalid payloads with 400", async () => {
        await (0, supertest_1.default)(server())
            .post("/v1/roles")
            .send({ description: "sem nome" })
            .expect(400)
            .expect((res) => expect(res.body.code).toBe("OA-400"));
    });
});
//# sourceMappingURL=roles.e2e-spec.js.map