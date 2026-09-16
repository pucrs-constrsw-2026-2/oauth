<?php

declare(strict_types=1);

namespace App\Application\UseCase\Role;

use App\Domain\Exception\ValidationException;
use App\Domain\Port\Inbound\DeleteRoleUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;

final class DeleteRoleUseCase implements DeleteRoleUseCaseInterface
{
    public function __construct(
        private readonly KeycloakRolePortInterface $rolePort
    ) {
    }

    public function execute(string $id): void
    {
        if (trim($id) === '') {
            throw new ValidationException('O ID do role é obrigatório.', ['id' => 'O ID do role é obrigatório.']);
        }

        $this->rolePort->disableRole($id);
    }
}
