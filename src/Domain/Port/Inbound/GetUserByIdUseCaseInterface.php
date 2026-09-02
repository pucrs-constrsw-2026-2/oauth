<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

use App\Application\DTO\User\UserDTO;

interface GetUserByIdUseCaseInterface
{
    public function execute(string $id): UserDTO;
}
