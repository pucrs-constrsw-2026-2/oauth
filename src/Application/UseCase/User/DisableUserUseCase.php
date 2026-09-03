<?php

declare(strict_types=1);

namespace App\Application\UseCase\User;

use App\Domain\Exception\ValidationException;
use App\Domain\Port\Inbound\DisableUserUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakUserPortInterface;

final class DisableUserUseCase implements DisableUserUseCaseInterface
{
    public function __construct(
        private readonly KeycloakUserPortInterface $userPort
    ) {
    }

    public function execute(string $id): void
    {
        if (trim($id) === '') {
            throw new ValidationException('O ID do usuário é obrigatório.');
        }

        $this->userPort->disableUser($id);
    }
}
