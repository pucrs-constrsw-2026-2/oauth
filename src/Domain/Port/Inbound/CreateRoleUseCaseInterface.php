<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

use App\Application\DTO\Role\CreateRoleDTO;
use App\Application\DTO\Role\RoleDTO;

interface CreateRoleUseCaseInterface
{
    public function execute(CreateRoleDTO $dto): RoleDTO;
}
