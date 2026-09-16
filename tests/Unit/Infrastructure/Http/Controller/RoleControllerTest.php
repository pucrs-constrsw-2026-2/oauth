<?php

declare(strict_types=1);

namespace App\Tests\Unit\Infrastructure\Http\Controller;

use App\Application\DTO\Role\CreateRoleDTO;
use App\Application\DTO\Role\RoleDTO;
use App\Application\DTO\Role\UpdateRoleDTO;
use App\Domain\Port\Inbound\CreateRoleUseCaseInterface;
use App\Domain\Port\Inbound\DeleteRoleUseCaseInterface;
use App\Domain\Port\Inbound\GetRoleByIdUseCaseInterface;
use App\Domain\Port\Inbound\ListRolesUseCaseInterface;
use App\Domain\Port\Inbound\UpdateRoleUseCaseInterface;
use App\Infrastructure\Http\Controller\RoleController;
use PHPUnit\Framework\TestCase;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;

final class RoleControllerTest extends TestCase
{
    private CreateRoleUseCaseInterface $createRoleUseCase;
    private ListRolesUseCaseInterface $listRolesUseCase;
    private GetRoleByIdUseCaseInterface $getRoleByIdUseCase;
    private UpdateRoleUseCaseInterface $updateRoleUseCase;
    private DeleteRoleUseCaseInterface $deleteRoleUseCase;
    private RoleController $controller;

    protected function setUp(): void
    {
        $this->createRoleUseCase = $this->createMock(CreateRoleUseCaseInterface::class);
        $this->listRolesUseCase = $this->createMock(ListRolesUseCaseInterface::class);
        $this->getRoleByIdUseCase = $this->createMock(GetRoleByIdUseCaseInterface::class);
        $this->updateRoleUseCase = $this->createMock(UpdateRoleUseCaseInterface::class);
        $this->deleteRoleUseCase = $this->createMock(DeleteRoleUseCaseInterface::class);

        $this->controller = new RoleController(
            $this->createRoleUseCase,
            $this->listRolesUseCase,
            $this->getRoleByIdUseCase,
            $this->updateRoleUseCase,
            $this->deleteRoleUseCase
        );
    }

    public function testCreateReturns201WithRoleJson(): void
    {
        $roleDTO = new RoleDTO(
            id: 'uuid-role-1',
            name: 'editor',
            description: 'Content Editor',
            enabled: true
        );

        $this->createRoleUseCase
            ->expects($this->once())
            ->method('execute')
            ->with($this->callback(function (CreateRoleDTO $dto) {
                return $dto->name === 'editor' && $dto->description === 'Content Editor';
            }))
            ->willReturn($roleDTO);

        $request = new Request(
            content: json_encode(['name' => 'editor', 'description' => 'Content Editor'], JSON_THROW_ON_ERROR)
        );

        $response = $this->controller->create($request);

        $this->assertSame(Response::HTTP_CREATED, $response->getStatusCode());
        $data = json_decode((string) $response->getContent(), true);
        $this->assertSame('uuid-role-1', $data['id']);
        $this->assertSame('editor', $data['name']);
        $this->assertSame('Content Editor', $data['description']);
        $this->assertTrue($data['enabled']);
    }

    public function testListReturns200WithArrayOfRoles(): void
    {
        $roles = [
            new RoleDTO(id: 'uuid-1', name: 'admin', description: 'Admin role', enabled: true),
            new RoleDTO(id: 'uuid-2', name: 'student', description: 'Student role', enabled: true),
        ];

        $this->listRolesUseCase
            ->expects($this->once())
            ->method('execute')
            ->willReturn($roles);

        $response = $this->controller->list();

        $this->assertSame(Response::HTTP_OK, $response->getStatusCode());
        $data = json_decode((string) $response->getContent(), true);
        $this->assertCount(2, $data);
        $this->assertSame('admin', $data[0]['name']);
        $this->assertSame('student', $data[1]['name']);
    }

    public function testGetReturns200WithRoleJson(): void
    {
        $roleDTO = new RoleDTO(
            id: 'uuid-123',
            name: 'coordinator',
            description: 'Coordinator role',
            enabled: true
        );

        $this->getRoleByIdUseCase
            ->expects($this->once())
            ->method('execute')
            ->with('uuid-123')
            ->willReturn($roleDTO);

        $response = $this->controller->get('uuid-123');

        $this->assertSame(Response::HTTP_OK, $response->getStatusCode());
        $data = json_decode((string) $response->getContent(), true);
        $this->assertSame('uuid-123', $data['id']);
        $this->assertSame('coordinator', $data['name']);
    }

    public function testUpdateReturns200WithRoleJson(): void
    {
        $roleDTO = new RoleDTO(
            id: 'uuid-123',
            name: 'coordinator-updated',
            description: 'Updated desc',
            enabled: true
        );

        $this->updateRoleUseCase
            ->expects($this->once())
            ->method('execute')
            ->with('uuid-123', $this->callback(function (UpdateRoleDTO $dto) {
                return $dto->name === 'coordinator-updated' && $dto->description === 'Updated desc';
            }), false)
            ->willReturn($roleDTO);

        $request = new Request(
            content: json_encode(['name' => 'coordinator-updated', 'description' => 'Updated desc'], JSON_THROW_ON_ERROR)
        );

        $response = $this->controller->update('uuid-123', $request);

        $this->assertSame(Response::HTTP_OK, $response->getStatusCode());
        $data = json_decode((string) $response->getContent(), true);
        $this->assertSame('uuid-123', $data['id']);
        $this->assertSame('coordinator-updated', $data['name']);
    }

    public function testPatchReturns200WithRoleJson(): void
    {
        $roleDTO = new RoleDTO(
            id: 'uuid-123',
            name: 'coordinator',
            description: 'Patched desc',
            enabled: true
        );

        $this->updateRoleUseCase
            ->expects($this->once())
            ->method('execute')
            ->with('uuid-123', $this->callback(function (UpdateRoleDTO $dto) {
                return $dto->description === 'Patched desc';
            }), true)
            ->willReturn($roleDTO);

        $request = new Request(
            content: json_encode(['description' => 'Patched desc'], JSON_THROW_ON_ERROR)
        );

        $response = $this->controller->patch('uuid-123', $request);

        $this->assertSame(Response::HTTP_OK, $response->getStatusCode());
        $data = json_decode((string) $response->getContent(), true);
        $this->assertSame('uuid-123', $data['id']);
        $this->assertSame('Patched desc', $data['description']);
    }

    public function testDeleteReturns204WithEmptyBody(): void
    {
        $this->deleteRoleUseCase
            ->expects($this->once())
            ->method('execute')
            ->with('uuid-123');

        $response = $this->controller->delete('uuid-123');

        $this->assertSame(Response::HTTP_NO_CONTENT, $response->getStatusCode());
    }
}
