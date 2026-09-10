import { KeycloakSettingsService } from '../config';
import { KeycloakAdminService } from './keycloak-admin.service';

describe('KeycloakAdminService', () => {
  const settings = {
    adminRealmUrl: 'http://keycloak/admin/realms/constrsw',
    serverUrl: 'http://keycloak',
    clientId: 'oauth',
    adminUser: 'admin',
    adminPassword: 'password',
  } as KeycloakSettingsService;

  let service: KeycloakAdminService;
  let fetchMock: jest.Spied<typeof fetch>;

  beforeEach(() => {
    service = new KeycloakAdminService(settings);
    fetchMock = jest.spyOn(global, 'fetch');
  });

  afterEach(() => {
    fetchMock.mockRestore();
  });

  it('maps an Admin API network failure to OA 503', async () => {
    fetchMock.mockRejectedValue(new Error('network down'));

    await expect(service.listRoles()).rejects.toMatchObject({
      status: 503,
      response: expect.objectContaining({
        error_code: 'OA-503',
        error_source: 'OAuthAPI',
      }),
    });
  });

  it('maps malformed client-role JSON to OA 502', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ access_token: 'admin-token' }))
      .mockResolvedValueOnce(jsonResponse([{ id: 'client-uuid', clientId: 'oauth' }]))
      .mockResolvedValueOnce(jsonResponse({ roles: [] }));

    await expect(service.listRoles()).rejects.toMatchObject({
      status: 502,
      response: expect.objectContaining({ error_code: 'OA-502' }),
    });
  });

  it('uses the client role mapping endpoint for assignment', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ access_token: 'admin-token' }))
      .mockResolvedValueOnce(jsonResponse([{ id: 'client-uuid', clientId: 'oauth' }]))
      .mockResolvedValueOnce(jsonResponse({ access_token: 'admin-token' }))
      .mockResolvedValueOnce(jsonResponse([
        { id: 'role-uuid', name: 'professor', clientRole: true, containerId: 'client-uuid' },
      ]))
      .mockResolvedValueOnce(jsonResponse({ access_token: 'admin-token' }))
      .mockResolvedValueOnce(jsonResponse({ id: 'user-uuid' }))
      .mockResolvedValueOnce(jsonResponse({ access_token: 'admin-token' }))
      .mockResolvedValueOnce(jsonResponse([{ id: 'client-uuid', clientId: 'oauth' }]))
      .mockResolvedValueOnce(jsonResponse({ access_token: 'admin-token' }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));

    await service.assignRole('user-uuid', 'professor');

    expect(fetchMock.mock.calls.at(-1)?.[0]).toBe(
      'http://keycloak/admin/realms/constrsw/users/user-uuid/role-mappings/clients/client-uuid',
    );
    expect(fetchMock.mock.calls.at(-1)?.[1]).toMatchObject({ method: 'POST' });
  });
});

function jsonResponse(value: unknown, status = 200): Response {
  return new Response(JSON.stringify(value), {
    status,
    headers: { 'content-type': 'application/json' },
  });
}