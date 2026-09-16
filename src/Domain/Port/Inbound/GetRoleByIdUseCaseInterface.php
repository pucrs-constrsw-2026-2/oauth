<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

use App\Application\DTO\Role\RoleDTO;

interface GetRoleByIdUseCaseInterface
{
    public function execute(string $id): RoleDTO;
}
