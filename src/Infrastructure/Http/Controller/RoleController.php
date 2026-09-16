<?php

declare(strict_types=1);

namespace App\Infrastructure\Http\Controller;

use App\Application\DTO\Role\CreateRoleDTO;
use App\Application\DTO\Role\RoleDTO;
use App\Application\DTO\Role\UpdateRoleDTO;
use App\Domain\Port\Inbound\CreateRoleUseCaseInterface;
use App\Domain\Port\Inbound\DeleteRoleUseCaseInterface;
use App\Domain\Port\Inbound\GetRoleByIdUseCaseInterface;
use App\Domain\Port\Inbound\ListRolesUseCaseInterface;
use App\Domain\Port\Inbound\UpdateRoleUseCaseInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;

final class RoleController extends AbstractController
{
    public function __construct(
        private readonly CreateRoleUseCaseInterface $createRoleUseCase,
        private readonly ListRolesUseCaseInterface $listRolesUseCase,
        private readonly GetRoleByIdUseCaseInterface $getRoleByIdUseCase,
        private readonly UpdateRoleUseCaseInterface $updateRoleUseCase,
        private readonly DeleteRoleUseCaseInterface $deleteRoleUseCase
    ) {
    }

    #[Route('/roles', name: 'role_create', methods: ['POST'])]
    public function create(Request $request): JsonResponse
    {
        $data = $this->extractRequestData($request);
        $dto = CreateRoleDTO::fromArray($data);
        $createdRole = $this->createRoleUseCase->execute($dto);

        return new JsonResponse($createdRole->toArray(), JsonResponse::HTTP_CREATED);
    }

    #[Route('/roles', name: 'role_list', methods: ['GET'])]
    public function list(): JsonResponse
    {
        $roles = $this->listRolesUseCase->execute();
        $payload = array_map(static fn (RoleDTO $role) => $role->toArray(), $roles);

        return new JsonResponse($payload, JsonResponse::HTTP_OK);
    }

    #[Route('/roles/{id}', name: 'role_get', methods: ['GET'])]
    public function get(string $id): JsonResponse
    {
        $role = $this->getRoleByIdUseCase->execute($id);

        return new JsonResponse($role->toArray(), JsonResponse::HTTP_OK);
    }

    #[Route('/roles/{id}', name: 'role_update', methods: ['PUT'])]
    public function update(string $id, Request $request): JsonResponse
    {
        $data = $this->extractRequestData($request);
        $dto = UpdateRoleDTO::fromArray($data);
        $updatedRole = $this->updateRoleUseCase->execute($id, $dto, false);

        return new JsonResponse($updatedRole->toArray(), JsonResponse::HTTP_OK);
    }

    #[Route('/roles/{id}', name: 'role_patch', methods: ['PATCH'])]
    public function patch(string $id, Request $request): JsonResponse
    {
        $data = $this->extractRequestData($request);
        $dto = UpdateRoleDTO::fromArray($data);
        $updatedRole = $this->updateRoleUseCase->execute($id, $dto, true);

        return new JsonResponse($updatedRole->toArray(), JsonResponse::HTTP_OK);
    }

    #[Route('/roles/{id}', name: 'role_delete', methods: ['DELETE'])]
    public function delete(string $id): JsonResponse
    {
        $this->deleteRoleUseCase->execute($id);

        return new JsonResponse(null, JsonResponse::HTTP_NO_CONTENT);
    }

    private function extractRequestData(Request $request): array
    {
        if ($request->request->count() > 0) {
            return $request->request->all();
        }

        $rawContent = $request->getContent();
        if (trim($rawContent) === '') {
            return [];
        }

        $decoded = json_decode($rawContent, true);
        return is_array($decoded) ? $decoded : [];
    }
}
