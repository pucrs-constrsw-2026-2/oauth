<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\Role;

use App\Application\DTO\Role\RoleDTO;
use App\Application\DTO\Role\UpdateRoleDTO;
use App\Application\UseCase\Role\UpdateRoleUseCase;
use App\Domain\Exception\RoleNotFoundException;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;
use PHPUnit\Framework\TestCase;

final class UpdateRoleUseCaseTest extends TestCase
{
    private KeycloakRolePortInterface $rolePort;
    private UpdateRoleUseCase $useCase;

    protected function setUp(): void
    {
        $this->rolePort = $this->createMock(KeycloakRolePortInterface::class);
        $this->useCase = new UpdateRoleUseCase($this->rolePort);
    }

    public function testExecuteFullUpdateSuccess(): void
    {
        $dto = new UpdateRoleDTO(name: 'novo-nome', description: 'nova desc');
        $expectedRole = new RoleDTO(id: 'uuid-1', name: 'novo-nome', description: 'nova desc', enabled: true);

        $this->rolePort->expects($this->once())
            ->method('updateRole')
            ->with('uuid-1', $dto)
            ->willReturn($expectedRole);

        $result = $this->useCase->execute('uuid-1', $dto, false);

        $this->assertSame('uuid-1', $result->id);
        $this->assertSame('novo-nome', $result->name);
        $this->assertSame('nova desc', $result->description);
        $this->assertTrue($result->enabled);
    }

    public function testExecuteFullUpdateThrowsValidationExceptionWhenNameIsEmpty(): void
    {
        $dto = new UpdateRoleDTO(name: '', description: 'nova desc');

        $this->expectException(ValidationException::class);
        $this->expectExceptionMessage('Dados inválidos para atualização do role.');

        $this->useCase->execute('uuid-1', $dto, false);
    }

    public function testExecuteFullUpdateThrowsValidationExceptionWhenNameIsNull(): void
    {
        $dto = new UpdateRoleDTO(name: null, description: 'nova desc');

        $this->expectException(ValidationException::class);

        $this->useCase->execute('uuid-1', $dto, false);
    }

    public function testExecuteThrowsValidationExceptionWhenIdIsEmpty(): void
    {
        $dto = new UpdateRoleDTO(name: 'nome');

        $this->expectException(ValidationException::class);
        $this->expectExceptionMessage('O ID do role é obrigatório.');

        $this->useCase->execute('   ', $dto, false);
    }

    public function testExecutePartialUpdateSuccessWithDescriptionOnly(): void
    {
        $dto = new UpdateRoleDTO(name: null, description: 'apenas desc alterada');
        $expectedRole = new RoleDTO(id: 'uuid-1', name: 'nome-mantido', description: 'apenas desc alterada', enabled: true);

        $this->rolePort->expects($this->once())
            ->method('updateRole')
            ->with('uuid-1', $dto)
            ->willReturn($expectedRole);

        $result = $this->useCase->execute('uuid-1', $dto, true);

        $this->assertSame('uuid-1', $result->id);
        $this->assertSame('apenas desc alterada', $result->description);
    }

    public function testExecutePartialUpdateSuccessWithNameOnly(): void
    {
        $dto = new UpdateRoleDTO(name: 'nome-alterado', description: null);
        $expectedRole = new RoleDTO(id: 'uuid-1', name: 'nome-alterado', description: 'desc mantida', enabled: true);

        $this->rolePort->expects($this->once())
            ->method('updateRole')
            ->with('uuid-1', $dto)
            ->willReturn($expectedRole);

        $result = $this->useCase->execute('uuid-1', $dto, true);

        $this->assertSame('uuid-1', $result->id);
        $this->assertSame('nome-alterado', $result->name);
    }

    public function testExecutePartialUpdateThrowsValidationExceptionWhenNoFieldsProvided(): void
    {
        $dto = new UpdateRoleDTO(name: null, description: null);

        $this->expectException(ValidationException::class);
        $this->expectExceptionMessage('Dados inválidos para atualização do role.');

        $this->useCase->execute('uuid-1', $dto, true);
    }

    public function testExecutePartialUpdateThrowsValidationExceptionWhenNameIsEmptyString(): void
    {
        $dto = new UpdateRoleDTO(name: '  ', description: null);

        $this->expectException(ValidationException::class);

        $this->useCase->execute('uuid-1', $dto, true);
    }

    public function testExecutePropagatesRoleNotFoundException(): void
    {
        $dto = new UpdateRoleDTO(name: 'nome');

        $this->rolePort->expects($this->once())
            ->method('updateRole')
            ->with('inexistente', $dto)
            ->willThrowException(new RoleNotFoundException());

        $this->expectException(RoleNotFoundException::class);

        $this->useCase->execute('inexistente', $dto, false);
    }
}
