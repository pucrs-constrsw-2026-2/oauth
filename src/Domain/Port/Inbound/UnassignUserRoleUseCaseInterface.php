<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

interface UnassignUserRoleUseCaseInterface
{
    public function execute(string $userId, string $roleId): void;
}
