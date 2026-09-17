<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\Role;

use App\Application\UseCase\Role\UnassignUserRoleUseCase;
use App\Domain\Exception\RoleNotFoundException;
use App\Domain\Exception\UserNotFoundException;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;
use PHPUnit\Framework\TestCase;

final class UnassignUserRoleUseCaseTest extends TestCase
{
    private KeycloakRolePortInterface $rolePort;
    private UnassignUserRoleUseCase $useCase;

    protected function setUp(): void
    {
        $this->rolePort = $this->createMock(KeycloakRolePortInterface::class);
        $this->useCase = new UnassignUserRoleUseCase($this->rolePort);
    }

    public function testExecuteSuccess(): void
    {
        $this->rolePort->expects($this->once())
            ->method('removeRoleFromUser')
            ->with('user-1', 'role-1');

        $this->useCase->execute('user-1', 'role-1');
    }

    public function testExecuteThrowsValidationExceptionWhenUserIdIsEmpty(): void
    {
        $this->expectException(ValidationException::class);
        $this->expectExceptionMessage('O ID do usuário é obrigatório.');

        $this->useCase->execute('  ', 'role-1');
    }

    public function testExecuteThrowsValidationExceptionWhenRoleIdIsEmpty(): void
    {
        $this->expectException(ValidationException::class);
        $this->expectExceptionMessage('O ID do role é obrigatório.');

        $this->useCase->execute('user-1', '  ');
    }

    public function testExecutePropagatesUserNotFoundException(): void
    {
        $this->rolePort->expects($this->once())
            ->method('removeRoleFromUser')
            ->with('user-inexistente', 'role-1')
            ->willThrowException(new UserNotFoundException());

        $this->expectException(UserNotFoundException::class);

        $this->useCase->execute('user-inexistente', 'role-1');
    }

    public function testExecutePropagatesRoleNotFoundException(): void
    {
        $this->rolePort->expects($this->once())
            ->method('removeRoleFromUser')
            ->with('user-1', 'role-inexistente')
            ->willThrowException(new RoleNotFoundException());

        $this->expectException(RoleNotFoundException::class);

        $this->useCase->execute('user-1', 'role-inexistente');
    }
}
