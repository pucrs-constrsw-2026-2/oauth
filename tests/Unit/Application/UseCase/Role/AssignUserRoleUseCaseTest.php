<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\Role;

use App\Application\DTO\Role\AssignRoleDTO;
use App\Application\UseCase\Role\AssignUserRoleUseCase;
use App\Domain\Exception\RoleNotFoundException;
use App\Domain\Exception\UserNotFoundException;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;
use PHPUnit\Framework\TestCase;

final class AssignUserRoleUseCaseTest extends TestCase
{
    private KeycloakRolePortInterface $rolePort;
    private AssignUserRoleUseCase $useCase;

    protected function setUp(): void
    {
        $this->rolePort = $this->createMock(KeycloakRolePortInterface::class);
        $this->useCase = new AssignUserRoleUseCase($this->rolePort);
    }

    public function testExecuteSuccessWithRoleId(): void
    {
        $dto = new AssignRoleDTO(roleId: 'uuid-role-1');

        $this->rolePort->expects($this->once())
            ->method('assignRoleToUser')
            ->with('user-uuid-1', 'uuid-role-1')
            ->willReturn([
                'success' => true,
                'userId' => 'user-uuid-1',
                'roleId' => 'uuid-role-1',
                'roleName' => 'professor',
            ]);

        $result = $this->useCase->execute('user-uuid-1', $dto);

        $this->assertTrue($result['success']);
        $this->assertSame('user-uuid-1', $result['userId']);
        $this->assertSame('uuid-role-1', $result['roleId']);
    }

    public function testExecuteSuccessWithName(): void
    {
        $dto = new AssignRoleDTO(name: 'administrator');

        $this->rolePort->expects($this->once())
            ->method('assignRoleToUser')
            ->with('user-uuid-2', 'administrator')
            ->willReturn([
                'success' => true,
                'userId' => 'user-uuid-2',
                'roleId' => 'uuid-role-admin',
                'roleName' => 'administrator',
            ]);

        $result = $this->useCase->execute('user-uuid-2', $dto);

        $this->assertTrue($result['success']);
        $this->assertSame('administrator', $result['roleName']);
    }

    public function testExecuteThrowsValidationExceptionWhenUserIdIsEmpty(): void
    {
        $dto = new AssignRoleDTO(name: 'professor');

        $this->expectException(ValidationException::class);
        $this->expectExceptionMessage('O ID do usuário é obrigatório.');

        $this->useCase->execute('   ', $dto);
    }

    public function testExecuteThrowsValidationExceptionWhenNoRoleIdentifierProvided(): void
    {
        $dto = new AssignRoleDTO(roleId: null, name: null);

        $this->expectException(ValidationException::class);
        $this->expectExceptionMessage('O identificador do papel (roleId ou name) é obrigatório.');

        $this->useCase->execute('user-1', $dto);
    }

    public function testExecutePropagatesUserNotFoundException(): void
    {
        $dto = new AssignRoleDTO(name: 'professor');

        $this->rolePort->expects($this->once())
            ->method('assignRoleToUser')
            ->with('user-inexistente', 'professor')
            ->willThrowException(new UserNotFoundException());

        $this->expectException(UserNotFoundException::class);

        $this->useCase->execute('user-inexistente', $dto);
    }

    public function testExecutePropagatesRoleNotFoundException(): void
    {
        $dto = new AssignRoleDTO(name: 'role-inexistente');

        $this->rolePort->expects($this->once())
            ->method('assignRoleToUser')
            ->with('user-1', 'role-inexistente')
            ->willThrowException(new RoleNotFoundException());

        $this->expectException(RoleNotFoundException::class);

        $this->useCase->execute('user-1', $dto);
    }
}
