<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\User;

use App\Application\DTO\User\UserDTO;
use App\Application\UseCase\User\GetUserByIdUseCase;
use App\Domain\Exception\UserNotFoundException;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Outbound\KeycloakUserPortInterface;
use PHPUnit\Framework\TestCase;

final class GetUserByIdUseCaseTest extends TestCase
{
    private KeycloakUserPortInterface $userPort;
    private GetUserByIdUseCase $useCase;

    protected function setUp(): void
    {
        $this->userPort = $this->createMock(KeycloakUserPortInterface::class);
        $this->useCase = new GetUserByIdUseCase($this->userPort);
    }

    public function testExecuteSuccessReturnsUser(): void
    {
        $user = new UserDTO(
            id: 'uuid-1',
            username: 'test@example.com',
            email: 'test@example.com',
            firstName: 'Test',
            lastName: 'User',
            enabled: true,
            roles: ['administrator']
        );

        $this->userPort
            ->expects($this->once())
            ->method('getUserById')
            ->with('uuid-1')
            ->willReturn($user);

        $result = $this->useCase->execute('uuid-1');

        $this->assertSame($user, $result);
        $this->assertSame('uuid-1', $result->id);
    }

    public function testExecuteThrowsValidationExceptionWhenIdIsEmpty(): void
    {
        $this->userPort->expects($this->never())->method('getUserById');

        $this->expectException(ValidationException::class);
        $this->useCase->execute('   ');
    }

    public function testExecuteThrowsUserNotFoundExceptionWhenUserIsNull(): void
    {
        $this->userPort
            ->expects($this->once())
            ->method('getUserById')
            ->with('nonexistent-id')
            ->willReturn(null);

        $this->expectException(UserNotFoundException::class);
        $this->expectExceptionCode(404);

        $this->useCase->execute('nonexistent-id');
    }
}
