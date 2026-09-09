<?php

declare(strict_types=1);

namespace App\Tests\Unit\Infrastructure\Http\Controller;

use App\Application\DTO\Authorization\AuthorizeRequestDTO;
use App\Application\DTO\Authorization\AuthorizeResponseDTO;
use App\Domain\Exception\InvalidTokenException;
use App\Domain\Port\Inbound\AuthorizeResourceUseCaseInterface;
use App\Infrastructure\Http\Controller\AuthorizationController;
use PHPUnit\Framework\TestCase;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;

final class AuthorizationControllerTest extends TestCase
{
    private AuthorizeResourceUseCaseInterface $authorizeUseCase;
    private AuthorizationController $controller;

    protected function setUp(): void
    {
        $this->authorizeUseCase = $this->createMock(AuthorizeResourceUseCaseInterface::class);
        $this->controller = new AuthorizationController($this->authorizeUseCase);
    }

    public function testAuthorizeSuccessReturns200WithPayload(): void
    {
        $responseDTO = new AuthorizeResponseDTO(
            authorized: true,
            resource: 'rooms',
            matchingRoles: ['administrator']
        );

        $this->authorizeUseCase
            ->expects($this->once())
            ->method('execute')
            ->with($this->callback(function (AuthorizeRequestDTO $dto) {
                return $dto->accessToken === 'sample-token' && $dto->resource === 'rooms';
            }))
            ->willReturn($responseDTO);

        $request = new Request(
            server: ['HTTP_AUTHORIZATION' => 'Bearer sample-token'],
            content: json_encode(['resource' => 'rooms'], JSON_THROW_ON_ERROR)
        );

        $response = $this->controller->authorize($request);

        $this->assertSame(Response::HTTP_OK, $response->getStatusCode());
        $data = json_decode((string) $response->getContent(), true);
        $this->assertTrue($data['authorized']);
        $this->assertSame('rooms', $data['resource']);
    }

    public function testAuthorizeMissingAuthorizationHeaderThrows401(): void
    {
        $request = new Request(
            content: json_encode(['resource' => 'rooms'], JSON_THROW_ON_ERROR)
        );

        $this->authorizeUseCase->expects($this->never())->method('execute');

        $this->expectException(InvalidTokenException::class);
        $this->expectExceptionCode(401);

        $this->controller->authorize($request);
    }

    public function testAuthorizeInvalidAuthorizationPrefixThrows401(): void
    {
        $request = new Request(
            server: ['HTTP_AUTHORIZATION' => 'Basic dXNlcjpwYXNz'],
            content: json_encode(['resource' => 'rooms'], JSON_THROW_ON_ERROR)
        );

        $this->authorizeUseCase->expects($this->never())->method('execute');

        $this->expectException(InvalidTokenException::class);
        $this->expectExceptionCode(401);

        $this->controller->authorize($request);
    }
}
