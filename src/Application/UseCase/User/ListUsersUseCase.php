<?php

declare(strict_types=1);

namespace App\Application\UseCase\User;

use App\Application\DTO\User\UserDTO;
use App\Domain\Port\Inbound\ListUsersUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakUserPortInterface;

final class ListUsersUseCase implements ListUsersUseCaseInterface
{
    public function __construct(
        private readonly KeycloakUserPortInterface $userPort
    ) {
    }

    /**
     * @return UserDTO[]
     */
    public function execute(): array
    {
        return $this->userPort->listActiveUsers();
    }
}
