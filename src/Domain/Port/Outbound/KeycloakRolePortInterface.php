<?php

declare(strict_types=1);

namespace App\Domain\Port\Outbound;

use App\Application\DTO\Role\CreateRoleDTO;
use App\Application\DTO\Role\RoleDTO;

interface KeycloakRolePortInterface
{
    public function createRole(CreateRoleDTO $dto): RoleDTO;

    /**
     * @return RoleDTO[]
     */
    public function listActiveRoles(): array;

    public function getRoleById(string $id): ?RoleDTO;
}
