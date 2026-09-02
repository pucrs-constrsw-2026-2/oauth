<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

interface DisableUserUseCaseInterface
{
    public function execute(string $id): void;
}
