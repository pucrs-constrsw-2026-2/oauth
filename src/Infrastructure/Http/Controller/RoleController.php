<?php

declare(strict_types=1);

namespace App\Infrastructure\Http\Controller;

use App\Application\DTO\Role\CreateRoleDTO;
use App\Application\DTO\Role\RoleDTO;
use App\Domain\Port\Inbound\CreateRoleUseCaseInterface;
use App\Domain\Port\Inbound\GetRoleByIdUseCaseInterface;
use App\Domain\Port\Inbound\ListRolesUseCaseInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;

final class RoleController extends AbstractController
{
    public function __construct(
        private readonly CreateRoleUseCaseInterface $createRoleUseCase,
        private readonly ListRolesUseCaseInterface $listRolesUseCase,
        private readonly GetRoleByIdUseCaseInterface $getRoleByIdUseCase
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
