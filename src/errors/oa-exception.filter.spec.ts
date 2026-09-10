import { HttpException, HttpStatus } from '@nestjs/common';

import { OaExceptionFilter } from './oa-exception.filter';

describe('OaExceptionFilter', () => {
  it('formats standard Nest exceptions as the OA envelope', () => {
    const json = jest.fn();
    const status = jest.fn().mockReturnValue({ json });
    const filter = new OaExceptionFilter();

    filter.catch(
      new HttpException('Access denied', HttpStatus.UNAUTHORIZED),
      httpHost(status),
    );

    expect(status).toHaveBeenCalledWith(401);
    expect(json).toHaveBeenCalledWith(
      expect.objectContaining({
        error_code: 'OA-401',
        error_description: 'Access denied',
        error_source: 'OAuthAPI',
      }),
    );
  });
});

function httpHost(status: jest.Mock): any {
  return {
    switchToHttp: () => ({
      getResponse: () => ({ status }),
      getRequest: () => ({ method: 'GET', originalUrl: '/roles' }),
    }),
  };
}