import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import * as request from 'supertest';

import { AppModule } from '../app.module';

describe('GET /health', () => {
  let app: INestApplication;

  beforeAll(async () => {
    process.env.KEYCLOAK_SERVER_URL = 'http://keycloak:8080';
    process.env.KEYCLOAK_CLIENT_SECRET = 'test-secret';

    const moduleRef = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleRef.createNestApplication();
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  it('returns HTTP 200', async () => {
    await request(app.getHttpServer())
      .get('/health')
      .expect(200)
      .expect({ status: 'ok' });
  });
});