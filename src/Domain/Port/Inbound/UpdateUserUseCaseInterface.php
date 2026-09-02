<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

use App\Application\DTO\User\UpdateUserDTO;

interface UpdateUserUseCaseInterface
{
    public function execute(string $id, UpdateUserDTO $dto): void;
}
