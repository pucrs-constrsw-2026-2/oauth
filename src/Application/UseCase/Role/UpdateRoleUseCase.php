<?php

declare(strict_types=1);

namespace App\Application\UseCase\Role;

use App\Application\DTO\Role\RoleDTO;
use App\Application\DTO\Role\UpdateRoleDTO;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Inbound\UpdateRoleUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;

final class UpdateRoleUseCase implements UpdateRoleUseCaseInterface
{
    public function __construct(
        private readonly KeycloakRolePortInterface $rolePort
    ) {
    }

    public function execute(string $id, UpdateRoleDTO $dto, bool $isPartial = false): RoleDTO
    {
        if (trim($id) === '') {
            throw new ValidationException('O ID do role é obrigatório.', ['id' => 'O ID do role é obrigatório.']);
        }

        $this->validateInput($dto, $isPartial);

        return $this->rolePort->updateRole($id, $dto);
    }

    private function validateInput(UpdateRoleDTO $dto, bool $isPartial): void
    {
        $errors = [];

        if (!$isPartial) {
            if ($dto->name === null || trim($dto->name) === '') {
                $errors['name'] = 'O campo name é obrigatório para atualização completa.';
            }
        } else {
            $hasAtLeastOne = false;

            if ($dto->name !== null) {
                if (trim($dto->name) === '') {
                    $errors['name'] = 'O campo name não pode ser vazio.';
                } else {
                    $hasAtLeastOne = true;
                }
            }

            if ($dto->description !== null) {
                $hasAtLeastOne = true;
            }

            if (!$hasAtLeastOne && empty($errors)) {
                $errors['payload'] = 'Pelo menos um campo deve ser informado para atualização parcial.';
            }
        }

        if (!empty($errors)) {
            throw new ValidationException('Dados inválidos para atualização do role.', $errors);
        }
    }
}
