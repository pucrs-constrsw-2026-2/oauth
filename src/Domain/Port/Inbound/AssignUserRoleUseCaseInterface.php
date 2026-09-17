<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

use App\Application\DTO\Role\AssignRoleDTO;

interface AssignUserRoleUseCaseInterface
{
    /**
     * @return array<string, mixed>
     */
    public function execute(string $userId, AssignRoleDTO $dto): array;
}
