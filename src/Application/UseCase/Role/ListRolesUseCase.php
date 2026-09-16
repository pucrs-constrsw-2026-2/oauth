<?php

declare(strict_types=1);

namespace App\Application\UseCase\Role;

use App\Application\DTO\Role\RoleDTO;
use App\Domain\Port\Inbound\ListRolesUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;

final class ListRolesUseCase implements ListRolesUseCaseInterface
{
    public function __construct(
        private readonly KeycloakRolePortInterface $rolePort
    ) {
    }

    /**
     * @return RoleDTO[]
     */
    public function execute(): array
    {
        return $this->rolePort->listActiveRoles();
    }
}
