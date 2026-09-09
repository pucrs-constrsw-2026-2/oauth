import { INestApplication, ValidationPipe } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { AppModule } from '../src/app.module';
import { OAuthExceptionFilter } from '../src/common/filters/oauth-exception.filter';
import { loginAs } from './utils/login';

/**
 * Testes de integracao do CRUD de /roles e da atribuicao/remocao de role em
 * usuario (/users/:userId/roles/:roleId), contra o Keycloak real.
 */
describe('Roles (e2e)', () => {
  let app: INestApplication;
  let adminToken: string;
  let studentUserId: string;
  let createdRoleId: string | undefined;
  const roleName = `role-integracao-${Date.now()}`;

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleRef.createNestApplication();
    app.useGlobalPipes(
      new ValidationPipe({ whitelist: true, transform: true, forbidNonWhitelisted: false }),
    );
    app.useGlobalFilters(new OAuthExceptionFilter());
    await app.init();

    adminToken = await loginAs(app, 'admin@pucrs.br', 'a12345678');

    const users = await request(app.getHttpServer())
      .get('/users')
      .set('Authorization', `Bearer ${adminToken}`);
    const student = (users.body as Array<{ id: string; username: string }>).find(
      (u) => u.username === 'student@pucrs.br',
    );
    if (!student) {
      throw new Error('Usuario student@pucrs.br nao encontrado no realm - realm oficial importado?');
    }
    studentUserId = student.id;
  });

  afterAll(async () => {
    if (createdRoleId) {
      await request(app.getHttpServer())
        .delete(`/roles/${createdRoleId}`)
        .set('Authorization', `Bearer ${adminToken}`)
        .catch(() => undefined);
    }
    await app.close();
  });

  it('POST /roles cria um role novo', async () => {
    const res = await request(app.getHttpServer())
      .post('/roles')
      .set('Authorization', `Bearer ${adminToken}`)
      .send({ name: roleName, description: 'role de teste de integracao' });

    expect(res.status).toBe(201);
    expect(res.body.name).toBe(roleName);
    expect(typeof res.body.id).toBe('string');
    createdRoleId = res.body.id;
  });

  it('POST /roles com nome ja existente retorna 409', async () => {
    const res = await request(app.getHttpServer())
      .post('/roles')
      .set('Authorization', `Bearer ${adminToken}`)
      .send({ name: roleName });

    expect(res.status).toBe(409);
  });

  it('GET /roles lista roles, incluindo o recem-criado', async () => {
    const res = await request(app.getHttpServer())
      .get('/roles')
      .set('Authorization', `Bearer ${adminToken}`);

    expect(res.status).toBe(200);
    expect(res.body.some((r: { id: string }) => r.id === createdRoleId)).toBe(true);
  });

  it('GET /roles/:id retorna o role criado', async () => {
    const res = await request(app.getHttpServer())
      .get(`/roles/${createdRoleId}`)
      .set('Authorization', `Bearer ${adminToken}`);

    expect(res.status).toBe(200);
    expect(res.body.id).toBe(createdRoleId);
  });

  it('GET /roles/:id com id inexistente retorna 404', async () => {
    const res = await request(app.getHttpServer())
      .get('/roles/00000000-0000-0000-0000-000000000000')
      .set('Authorization', `Bearer ${adminToken}`);

    expect(res.status).toBe(404);
  });

  it('PUT /roles/:id atualiza o role (completo)', async () => {
    const res = await request(app.getHttpServer())
      .put(`/roles/${createdRoleId}`)
      .set('Authorization', `Bearer ${adminToken}`)
      .send({ name: roleName, description: 'descricao atualizada via PUT' });

    expect(res.status).toBe(200);
  });

  it('PATCH /roles/:id atualiza o role (parcial)', async () => {
    const res = await request(app.getHttpServer())
      .patch(`/roles/${createdRoleId}`)
      .set('Authorization', `Bearer ${adminToken}`)
      .send({ description: 'descricao atualizada via PATCH' });

    expect(res.status).toBe(200);
  });

  it('POST /users/:userId/roles/:roleId atribui o role ao usuario', async () => {
    const res = await request(app.getHttpServer())
      .post(`/users/${studentUserId}/roles/${createdRoleId}`)
      .set('Authorization', `Bearer ${adminToken}`);

    expect(res.status).toBe(204);
  });

  it('POST .../roles/:roleId com role inexistente retorna 404', async () => {
    const res = await request(app.getHttpServer())
      .post(`/users/${studentUserId}/roles/00000000-0000-0000-0000-000000000000`)
      .set('Authorization', `Bearer ${adminToken}`);

    expect(res.status).toBe(404);
  });

  it('POST .../roles/:roleId com usuario inexistente retorna 404', async () => {
    const res = await request(app.getHttpServer())
      .post(`/users/00000000-0000-0000-0000-000000000000/roles/${createdRoleId}`)
      .set('Authorization', `Bearer ${adminToken}`);

    expect(res.status).toBe(404);
  });

  it('DELETE /users/:userId/roles/:roleId remove a atribuicao', async () => {
    const res = await request(app.getHttpServer())
      .delete(`/users/${studentUserId}/roles/${createdRoleId}`)
      .set('Authorization', `Bearer ${adminToken}`);

    expect(res.status).toBe(204);
  });

  it('DELETE /roles/:id exclui o role de fato (nao ha soft-delete para roles)', async () => {
    const res = await request(app.getHttpServer())
      .delete(`/roles/${createdRoleId}`)
      .set('Authorization', `Bearer ${adminToken}`);
    expect(res.status).toBe(204);

    const check = await request(app.getHttpServer())
      .get(`/roles/${createdRoleId}`)
      .set('Authorization', `Bearer ${adminToken}`);
    expect(check.status).toBe(404);

    createdRoleId = undefined; // ja foi removido, nao tentar de novo no afterAll
  });
});
