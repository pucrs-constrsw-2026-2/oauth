<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

use App\Application\DTO\User\UpdatePasswordDTO;

interface UpdatePasswordUseCaseInterface
{
    public function execute(string $id, UpdatePasswordDTO $dto): void;
}
