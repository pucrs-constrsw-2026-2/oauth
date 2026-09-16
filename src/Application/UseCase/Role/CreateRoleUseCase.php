<?php

declare(strict_types=1);

namespace App\Application\UseCase\Role;

use App\Application\DTO\Role\CreateRoleDTO;
use App\Application\DTO\Role\RoleDTO;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Inbound\CreateRoleUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;

final class CreateRoleUseCase implements CreateRoleUseCaseInterface
{
    public function __construct(
        private readonly KeycloakRolePortInterface $rolePort
    ) {
    }

    public function execute(CreateRoleDTO $dto): RoleDTO
    {
        $this->validateInput($dto);

        return $this->rolePort->createRole($dto);
    }

    private function validateInput(CreateRoleDTO $dto): void
    {
        $errors = [];

        if (trim($dto->name) === '') {
            $errors['name'] = 'O campo name é obrigatório.';
        }

        if (!empty($errors)) {
            throw new ValidationException('Dados de role inválidos ou campos obrigatórios ausentes.', $errors);
        }
    }
}
