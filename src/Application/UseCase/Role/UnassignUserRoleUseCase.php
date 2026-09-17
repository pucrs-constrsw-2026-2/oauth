<?php

declare(strict_types=1);

namespace App\Application\UseCase\Role;

use App\Domain\Exception\ValidationException;
use App\Domain\Port\Inbound\UnassignUserRoleUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;

final class UnassignUserRoleUseCase implements UnassignUserRoleUseCaseInterface
{
    public function __construct(
        private readonly KeycloakRolePortInterface $rolePort
    ) {
    }

    public function execute(string $userId, string $roleId): void
    {
        if (trim($userId) === '') {
            throw new ValidationException('O ID do usuário é obrigatório.', ['userId' => 'O ID do usuário é obrigatório.']);
        }

        if (trim($roleId) === '') {
            throw new ValidationException('O ID do role é obrigatório.', ['roleId' => 'O ID do role é obrigatório.']);
        }

        $this->rolePort->removeRoleFromUser($userId, $roleId);
    }
}
