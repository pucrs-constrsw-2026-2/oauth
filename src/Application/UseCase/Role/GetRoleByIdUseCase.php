<?php

declare(strict_types=1);

namespace App\Application\UseCase\Role;

use App\Application\DTO\Role\RoleDTO;
use App\Domain\Exception\RoleNotFoundException;
use App\Domain\Port\Inbound\GetRoleByIdUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;

final class GetRoleByIdUseCase implements GetRoleByIdUseCaseInterface
{
    public function __construct(
        private readonly KeycloakRolePortInterface $rolePort
    ) {
    }

    public function execute(string $id): RoleDTO
    {
        $role = $this->rolePort->getRoleById($id);

        if ($role === null) {
            throw new RoleNotFoundException("Role com ID '{$id}' não encontrado.");
        }

        return $role;
    }
}
