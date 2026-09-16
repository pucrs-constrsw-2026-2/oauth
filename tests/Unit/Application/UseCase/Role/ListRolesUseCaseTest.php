<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\Role;

use App\Application\DTO\Role\RoleDTO;
use App\Application\UseCase\Role\ListRolesUseCase;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;
use PHPUnit\Framework\TestCase;

final class ListRolesUseCaseTest extends TestCase
{
    private KeycloakRolePortInterface $rolePort;
    private ListRolesUseCase $useCase;

    protected function setUp(): void
    {
        $this->rolePort = $this->createMock(KeycloakRolePortInterface::class);
        $this->useCase = new ListRolesUseCase($this->rolePort);
    }

    public function testExecuteReturnsArrayOfRoles(): void
    {
        $roles = [
            new RoleDTO(id: 'uuid-1', name: 'admin', description: 'Administrator', enabled: true),
            new RoleDTO(id: 'uuid-2', name: 'student', description: 'Student', enabled: true),
        ];

        $this->rolePort
            ->expects($this->once())
            ->method('listActiveRoles')
            ->willReturn($roles);

        $result = $this->useCase->execute();

        $this->assertSame($roles, $result);
        $this->assertCount(2, $result);
        $this->assertSame('admin', $result[0]->name);
        $this->assertSame('student', $result[1]->name);
    }
}
