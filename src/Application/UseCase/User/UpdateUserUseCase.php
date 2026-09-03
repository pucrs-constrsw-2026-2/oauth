<?php

declare(strict_types=1);

namespace App\Application\UseCase\User;

use App\Application\DTO\User\UpdateUserDTO;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Inbound\UpdateUserUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakUserPortInterface;

final class UpdateUserUseCase implements UpdateUserUseCaseInterface
{
    public function __construct(
        private readonly KeycloakUserPortInterface $userPort
    ) {
    }

    public function execute(string $id, UpdateUserDTO $dto): void
    {
        if (trim($id) === '') {
            throw new ValidationException('O ID do usuário é obrigatório.');
        }

        $this->validateInput($dto);

        $this->userPort->updateUser($id, $dto);
    }

    private function validateInput(UpdateUserDTO $dto): void
    {
        $errors = [];
        $hasAtLeastOneField = false;

        if ($dto->firstName !== null) {
            if (trim($dto->firstName) === '') {
                $errors['firstName'] = 'O campo firstName não pode ser vazio.';
            } else {
                $hasAtLeastOneField = true;
            }
        }

        if ($dto->lastName !== null) {
            if (trim($dto->lastName) === '') {
                $errors['lastName'] = 'O campo lastName não pode ser vazio.';
            } else {
                $hasAtLeastOneField = true;
            }
        }

        if ($dto->email !== null) {
            if (trim($dto->email) === '') {
                $errors['email'] = 'O campo email não pode ser vazio.';
            } elseif (!filter_var($dto->email, FILTER_VALIDATE_EMAIL)) {
                $errors['email'] = 'O campo email deve conter um endereço de e-mail válido.';
            } else {
                $hasAtLeastOneField = true;
            }
        }

        if (!$hasAtLeastOneField && empty($errors)) {
            throw new ValidationException('Ao menos um campo (firstName, lastName ou email) deve ser informado para atualização.');
        }

        if (!empty($errors)) {
            throw new ValidationException('Dados de atualização inválidos.', $errors);
        }
    }
}
