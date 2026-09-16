<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

use App\Application\DTO\Role\RoleDTO;

interface ListRolesUseCaseInterface
{
    /**
     * @return RoleDTO[]
     */
    public function execute(): array;
}
