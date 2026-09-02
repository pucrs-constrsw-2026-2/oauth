<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

use App\Application\DTO\User\UserDTO;

interface ListUsersUseCaseInterface
{
    /**
     * @return UserDTO[]
     */
    public function execute(): array;
}
