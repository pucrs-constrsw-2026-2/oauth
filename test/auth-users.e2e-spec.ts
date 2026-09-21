import { INestApplication, ValidationPipe } from "@nestjs/common";
import { Test } from "@nestjs/testing";
import request from "supertest";
import { AppModule } from "../src/app.module";
import { ProblemDetailsFilter } from "../src/common/problem-details.filter";

const USERS_BASE = "/admin/realms/constrsw/users";

function createResponse(status: number, body?: unknown): Response {
  const text = body === undefined ? "" : JSON.stringify(body);
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body ?? {},
    text: async () => text,
  } as unknown as Response;
}

function createKeycloakFake() {
  const users = new Map([
    [
      "u1",
      {
        id: "u1",
        username: "one@pucrs.br",
        firstName: "One",
        lastName: "User",
        enabled: true,
      },
    ],
    ["u2", { id: "u2", username: "two@pucrs.br", enabled: false }],
    ["sa", { id: "sa", username: "service-account-oauth", enabled: true }],
  ]);

  const impl = async (input: URL | RequestInfo): Promise<Response> => {
    const url = new URL(String(input));
    if (url.pathname.endsWith("/protocol/openid-connect/token")) {
      return createResponse(200, {
        token_type: "Bearer",
        access_token: "test-token",
        refresh_token: "refresh-token",
        expires_in: 300,
        refresh_expires_in: 1800,
      });
    }
    if (url.pathname === USERS_BASE) {
      const enabled = url.searchParams.get("enabled");
      return createResponse(
        200,
        [...users.values()].filter(
          (user) => enabled === null || String(user.enabled) === enabled,
        ),
      );
    }
    const match = url.pathname.match(
      /^\/admin\/realms\/constrsw\/users\/(.+)$/,
    );
    if (match) {
      const user = users.get(decodeURIComponent(match[1]));
      return user ? createResponse(200, user) : createResponse(404);
    }
    return createResponse(404);
  };

  return { impl };
}

describe("Authentication and users (e2e)", () => {
  let app: INestApplication;

  beforeEach(async () => {
    jest
      .spyOn(global, "fetch")
      .mockImplementation(createKeycloakFake().impl as unknown as typeof fetch);
    const moduleRef = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();
    app = moduleRef.createNestApplication();
    app.useGlobalPipes(
      new ValidationPipe({ whitelist: true, transform: true }),
    );
    app.useGlobalFilters(new ProblemDetailsFilter());
    await app.init();
  });

  afterEach(async () => {
    await app.close();
    jest.restoreAllMocks();
  });

  const server = () => app.getHttpServer();

  it("authenticates with multipart form data and returns the token contract", async () => {
    await request(server())
      .post("/login")
      .field("username", "one@pucrs.br")
      .field("password", "secret")
      .expect(201)
      .expect((res) => {
        expect(res.body).toMatchObject({
          token_type: "Bearer",
          access_token: "test-token",
          expires_in: 300,
          refresh_token: "refresh-token",
          refresh_expires_in: 1800,
        });
        expect(res.headers["set-cookie"]).toBeDefined();
      });
  });

  it("refreshes and logs out the session cookie", async () => {
    const agent = request.agent(server());
    await agent
      .post("/login")
      .field("username", "one@pucrs.br")
      .field("password", "secret")
      .expect(201);

    await agent.post("/refresh").expect(201);
    await agent.post("/logout").expect(201).expect({ status: "signed_out" });
  });

  it("rejects invalid login payloads before calling Keycloak", async () => {
    await request(server())
      .post("/login")
      .field("username", "invalid")
      .field("password", "secret")
      .expect(400)
      .expect((res) => expect(res.body.error_code).toBe("OA-400"));
  });

  it("lists users, filters enabled and excludes service accounts", async () => {
    await request(server())
      .get("/users")
      .set("Authorization", "Bearer test-token")
      .expect(200)
      .expect((res) =>
        expect(res.body.map((user: { id: string }) => user.id)).toEqual([
          "u1",
          "u2",
        ]),
      );

    await request(server())
      .get("/users?enabled=true")
      .set("Authorization", "Bearer test-token")
      .expect(200)
      .expect((res) =>
        expect(res.body.map((user: { id: string }) => user.id)).toEqual(["u1"]),
      );
  });

  it("gets one user and maps an upstream 404", async () => {
    await request(server())
      .get("/users/u1")
      .set("Authorization", "Bearer test-token")
      .expect(200)
      .expect((res) =>
        expect(res.body).toEqual({
          id: "u1",
          username: "one@pucrs.br",
          "first-name": "One",
          "last-name": "User",
          enabled: true,
        }),
      );

    await request(server())
      .get("/users/missing")
      .set("Authorization", "Bearer test-token")
      .expect(404)
      .expect((res) => expect(res.body.error_code).toBe("OA-404"));
  });

  it("requires a bearer token and surfaces upstream forbidden responses", async () => {
    await request(server())
      .get("/users")
      .expect(401)
      .expect((res) => expect(res.body.error_code).toBe("OA-401"));

    jest.spyOn(global, "fetch").mockResolvedValue(createResponse(403));
    await request(server())
      .get("/users")
      .set("Authorization", "Bearer denied")
      .expect(403)
      .expect((res) => expect(res.body.error_code).toBe("OA-403"));
  });
});
