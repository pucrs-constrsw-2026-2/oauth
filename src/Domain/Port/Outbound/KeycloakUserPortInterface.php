<?php

declare(strict_types=1);

namespace App\Domain\Port\Outbound;

use App\Application\DTO\User\CreateUserDTO;
use App\Application\DTO\User\UpdatePasswordDTO;
use App\Application\DTO\User\UpdateUserDTO;
use App\Application\DTO\User\UserDTO;

interface KeycloakUserPortInterface
{
    public function createUser(CreateUserDTO $dto): UserDTO;

    /**
     * @return UserDTO[]
     */
    public function listActiveUsers(): array;

    public function getUserById(string $id): ?UserDTO;

    public function updateUser(string $id, UpdateUserDTO $dto): void;

    public function updatePassword(string $id, UpdatePasswordDTO $dto): void;

    public function disableUser(string $id): void;
}
