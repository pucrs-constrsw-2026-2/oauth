<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\Role;

use App\Application\UseCase\Role\DeleteRoleUseCase;
use App\Domain\Exception\RoleNotFoundException;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;
use PHPUnit\Framework\TestCase;

final class DeleteRoleUseCaseTest extends TestCase
{
    private KeycloakRolePortInterface $rolePort;
    private DeleteRoleUseCase $useCase;

    protected function setUp(): void
    {
        $this->rolePort = $this->createMock(KeycloakRolePortInterface::class);
        $this->useCase = new DeleteRoleUseCase($this->rolePort);
    }

    public function testExecuteSuccess(): void
    {
        $this->rolePort->expects($this->once())
            ->method('disableRole')
            ->with('uuid-1');

        $this->useCase->execute('uuid-1');
    }

    public function testExecuteThrowsValidationExceptionWhenIdIsEmpty(): void
    {
        $this->expectException(ValidationException::class);
        $this->expectExceptionMessage('O ID do role é obrigatório.');

        $this->useCase->execute('   ');
    }

    public function testExecutePropagatesRoleNotFoundException(): void
    {
        $this->rolePort->expects($this->once())
            ->method('disableRole')
            ->with('inexistente')
            ->willThrowException(new RoleNotFoundException());

        $this->expectException(RoleNotFoundException::class);

        $this->useCase->execute('inexistente');
    }
}
