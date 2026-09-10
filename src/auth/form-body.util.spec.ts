import type { Request } from 'express';

import { OaException } from '../errors';
import { assertSupportedFormContentType, requireFormField } from './form-body.util';

function requestWithContentType(contentType?: string): Request {
  return { headers: { 'content-type': contentType } } as unknown as Request;
}

describe('assertSupportedFormContentType', () => {
  it.each([
    'multipart/form-data; boundary=----abc',
    'application/x-www-form-urlencoded',
    'application/x-www-form-urlencoded; charset=UTF-8',
  ])('accepts %s', (contentType) => {
    expect(() => assertSupportedFormContentType(requestWithContentType(contentType))).not.toThrow();
  });

  it.each([
    ['application/json', 'application/json'],
    ['missing header', undefined],
    ['plain text', 'text/plain'],
  ])('rejects %s with a local OA-400', (_label, contentType) => {
    expect(() => assertSupportedFormContentType(requestWithContentType(contentType))).toThrow(
      OaException,
    );
  });
});

describe('requireFormField', () => {
  it('returns the trimmed-nonblank field value', () => {
    expect(requireFormField({ username: 'aluno@pucrs.br' }, 'username')).toBe(
      'aluno@pucrs.br',
    );
  });

  it.each([
    ['missing', {}],
    ['blank', { username: '   ' }],
    ['non-string', { username: 42 }],
  ])('throws OA-400 when the field is %s', (_label, body) => {
    expect(() => requireFormField(body, 'username')).toThrow(OaException);
  });

  it('ignores extra fields such as client_id or grant_type', () => {
    const body = {
      username: 'aluno@pucrs.br',
      password: 'secret',
      client_id: 'someone-elses-client',
      grant_type: 'password',
    };

    expect(requireFormField(body, 'username')).toBe('aluno@pucrs.br');
    expect(requireFormField(body, 'password')).toBe('secret');
  });
});
