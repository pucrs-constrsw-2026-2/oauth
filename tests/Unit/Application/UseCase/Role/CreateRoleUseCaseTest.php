<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\Role;

use App\Application\DTO\Role\CreateRoleDTO;
use App\Application\DTO\Role\RoleDTO;
use App\Application\UseCase\Role\CreateRoleUseCase;
use App\Domain\Exception\RoleAlreadyExistsException;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;
use PHPUnit\Framework\TestCase;

final class CreateRoleUseCaseTest extends TestCase
{
    private KeycloakRolePortInterface $rolePort;
    private CreateRoleUseCase $useCase;

    protected function setUp(): void
    {
        $this->rolePort = $this->createMock(KeycloakRolePortInterface::class);
        $this->useCase = new CreateRoleUseCase($this->rolePort);
    }

    public function testExecuteSuccessReturnsCreatedRole(): void
    {
        $dto = new CreateRoleDTO(
            name: 'editor',
            description: 'Editor de Conteúdo'
        );

        $expectedRole = new RoleDTO(
            id: 'uuid-role-123',
            name: 'editor',
            description: 'Editor de Conteúdo',
            enabled: true
        );

        $this->rolePort
            ->expects($this->once())
            ->method('createRole')
            ->with($dto)
            ->willReturn($expectedRole);

        $result = $this->useCase->execute($dto);

        $this->assertSame($expectedRole, $result);
        $this->assertSame('uuid-role-123', $result->id);
        $this->assertSame('editor', $result->name);
        $this->assertSame('Editor de Conteúdo', $result->description);
        $this->assertTrue($result->enabled);
    }

    public function testExecuteThrowsValidationExceptionWhenNameIsEmpty(): void
    {
        $dto = new CreateRoleDTO(name: '   ', description: 'Alguma descrição');

        $this->expectException(ValidationException::class);
        $this->expectExceptionMessage('Dados de role inválidos ou campos obrigatórios ausentes.');

        try {
            $this->useCase->execute($dto);
        } catch (ValidationException $e) {
            $errors = $e->getErrors();
            $this->assertArrayHasKey('name', $errors);
            $this->assertSame('O campo name é obrigatório.', $errors['name']);
            throw $e;
        }
    }

    public function testExecutePropagatesRoleAlreadyExistsException(): void
    {
        $dto = new CreateRoleDTO(name: 'editor');

        $this->rolePort
            ->expects($this->once())
            ->method('createRole')
            ->with($dto)
            ->willThrowException(new RoleAlreadyExistsException("Já existe um role com o nome 'editor'."));

        $this->expectException(RoleAlreadyExistsException::class);
        $this->expectExceptionMessage("Já existe um role com o nome 'editor'.");

        $this->useCase->execute($dto);
    }
}
