<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\Authorization;

use App\Application\DTO\Authorization\AuthorizeRequestDTO;
use App\Application\DTO\Authorization\AuthorizeResponseDTO;
use App\Application\UseCase\Authorization\AuthorizeResourceUseCase;
use App\Domain\Exception\AccessDeniedException;
use App\Domain\Exception\InvalidTokenException;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Outbound\KeycloakAuthorizationPortInterface;
use PHPUnit\Framework\TestCase;

final class AuthorizeResourceUseCaseTest extends TestCase
{
    private KeycloakAuthorizationPortInterface $authorizationPort;
    private AuthorizeResourceUseCase $useCase;

    protected function setUp(): void
    {
        $this->authorizationPort = $this->createMock(KeycloakAuthorizationPortInterface::class);
        $this->useCase = new AuthorizeResourceUseCase($this->authorizationPort);
    }

    public function testExecuteSuccessWhenAuthorized(): void
    {
        $dto = new AuthorizeRequestDTO(
            accessToken: 'valid-token',
            resource: 'rooms'
        );

        $expectedResponse = new AuthorizeResponseDTO(
            authorized: true,
            resource: 'rooms',
            matchingRoles: ['administrator'],
            reason: 'Acesso autorizado.'
        );

        $this->authorizationPort
            ->expects($this->once())
            ->method('validateAccess')
            ->with($dto)
            ->willReturn($expectedResponse);

        $result = $this->useCase->execute($dto);

        $this->assertSame($expectedResponse, $result);
        $this->assertTrue($result->authorized);
        $this->assertSame('rooms', $result->resource);
    }

    public function testExecuteThrowsAccessDeniedExceptionWhenNotAuthorized(): void
    {
        $dto = new AuthorizeRequestDTO(
            accessToken: 'valid-token',
            resource: 'rooms'
        );

        $deniedResponse = new AuthorizeResponseDTO(
            authorized: false,
            resource: 'rooms',
            matchingRoles: [],
            reason: 'Papel do usuário não possui permissão para rooms.'
        );

        $this->authorizationPort
            ->expects($this->once())
            ->method('validateAccess')
            ->with($dto)
            ->willReturn($deniedResponse);

        $this->expectException(AccessDeniedException::class);
        $this->expectExceptionCode(403);
        $this->expectExceptionMessage('Papel do usuário não possui permissão para rooms.');

        $this->useCase->execute($dto);
    }

    public function testExecuteThrowsInvalidTokenExceptionWhenTokenIsEmpty(): void
    {
        $dto = new AuthorizeRequestDTO(
            accessToken: '   ',
            resource: 'rooms'
        );

        $this->authorizationPort->expects($this->never())->method('validateAccess');

        $this->expectException(InvalidTokenException::class);
        $this->expectExceptionCode(401);

        $this->useCase->execute($dto);
    }

    public function testExecuteThrowsValidationExceptionWhenResourceIsEmpty(): void
    {
        $dto = new AuthorizeRequestDTO(
            accessToken: 'valid-token',
            resource: '   '
        );

        $this->authorizationPort->expects($this->never())->method('validateAccess');

        $this->expectException(ValidationException::class);
        $this->expectExceptionCode(400);

        try {
            $this->useCase->execute($dto);
        } catch (ValidationException $e) {
            $this->assertSame('VALIDATION_ERROR', $e->getErrorCode());
            $this->assertArrayHasKey('resource', $e->getErrors());
            throw $e;
        }
    }
}
