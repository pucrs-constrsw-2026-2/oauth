<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

interface DeleteRoleUseCaseInterface
{
    public function execute(string $id): void;
}
