<?php

declare(strict_types=1);

namespace App\Application\UseCase\User;

use App\Application\DTO\User\UserDTO;
use App\Domain\Exception\UserNotFoundException;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Inbound\GetUserByIdUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakUserPortInterface;

final class GetUserByIdUseCase implements GetUserByIdUseCaseInterface
{
    public function __construct(
        private readonly KeycloakUserPortInterface $userPort
    ) {
    }

    public function execute(string $id): UserDTO
    {
        if (trim($id) === '') {
            throw new ValidationException('O ID do usuário é obrigatório.');
        }

        $user = $this->userPort->getUserById($id);

        if ($user === null) {
            throw new UserNotFoundException();
        }

        return $user;
    }
}
