import { INestApplication } from '@nestjs/common';
import request from 'supertest';

/**
 * Faz login via POST /login (a propria API, ja rodando dentro do processo
 * de teste) e devolve o access_token. Usado pelos testes de integracao pra
 * obter tokens reais do Keycloak antes de chamar as rotas administrativas.
 */
export async function loginAs(
  app: INestApplication,
  username: string,
  password: string,
): Promise<string> {
  const res = await request(app.getHttpServer())
    .post('/login')
    .field('username', username)
    .field('password', password);

  if (res.status !== 201) {
    throw new Error(
      `Falha ao autenticar como ${username} nos testes de integracao (status ${res.status}, body: ${JSON.stringify(
        res.body,
      )}). Confirme que o Keycloak esta rodando localmente (docker compose up -d, na raiz ` +
        'do repo base) e que o realm "constrsw" com os usuarios de teste do professor esta importado.',
    );
  }

  return res.body.access_token as string;
}
