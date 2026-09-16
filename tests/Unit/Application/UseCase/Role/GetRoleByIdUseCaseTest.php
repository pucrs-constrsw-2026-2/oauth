<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\Role;

use App\Application\DTO\Role\RoleDTO;
use App\Application\UseCase\Role\GetRoleByIdUseCase;
use App\Domain\Exception\RoleNotFoundException;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;
use PHPUnit\Framework\TestCase;

final class GetRoleByIdUseCaseTest extends TestCase
{
    private KeycloakRolePortInterface $rolePort;
    private GetRoleByIdUseCase $useCase;

    protected function setUp(): void
    {
        $this->rolePort = $this->createMock(KeycloakRolePortInterface::class);
        $this->useCase = new GetRoleByIdUseCase($this->rolePort);
    }

    public function testExecuteSuccessReturnsRole(): void
    {
        $role = new RoleDTO(
            id: 'uuid-role-123',
            name: 'coordinator',
            description: 'Course Coordinator',
            enabled: true
        );

        $this->rolePort
            ->expects($this->once())
            ->method('getRoleById')
            ->with('uuid-role-123')
            ->willReturn($role);

        $result = $this->useCase->execute('uuid-role-123');

        $this->assertSame($role, $result);
        $this->assertSame('uuid-role-123', $result->id);
        $this->assertSame('coordinator', $result->name);
    }

    public function testExecuteThrowsRoleNotFoundExceptionWhenRoleDoesNotExist(): void
    {
        $this->rolePort
            ->expects($this->once())
            ->method('getRoleById')
            ->with('non-existent-id')
            ->willReturn(null);

        $this->expectException(RoleNotFoundException::class);
        $this->expectExceptionMessage("Role com ID 'non-existent-id' não encontrado.");

        $this->useCase->execute('non-existent-id');
    }
}
