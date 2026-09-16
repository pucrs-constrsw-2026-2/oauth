<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

use App\Application\DTO\Role\RoleDTO;
use App\Application\DTO\Role\UpdateRoleDTO;

interface UpdateRoleUseCaseInterface
{
    public function execute(string $id, UpdateRoleDTO $dto, bool $isPartial = false): RoleDTO;
}
