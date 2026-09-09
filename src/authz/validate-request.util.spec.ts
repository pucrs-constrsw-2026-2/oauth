import { OaException } from '../errors';
import { AUTHZ_RESOURCE_NAMES } from './authz-resource';
import { requireAuthzResourceName } from './validate-request.util';

describe('requireAuthzResourceName', () => {
  it.each(AUTHZ_RESOURCE_NAMES)('accepts the known resource "%s"', (resource) => {
    expect(requireAuthzResourceName({ resource })).toBe(resource);
  });

  it.each([
    ['unknown name', { resource: 'invoices' }],
    ['missing field', {}],
    ['non-string resource', { resource: 42 }],
    ['non-object body', 'classes'],
    ['null body', null],
  ])('rejects %s with OA-400', (_label, body) => {
    expect(() => requireAuthzResourceName(body)).toThrow(OaException);
  });
});
