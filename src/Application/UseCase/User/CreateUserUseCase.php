<?php

declare(strict_types=1);

namespace App\Application\UseCase\User;

use App\Application\DTO\User\CreateUserDTO;
use App\Application\DTO\User\UserDTO;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Inbound\CreateUserUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakUserPortInterface;

final class CreateUserUseCase implements CreateUserUseCaseInterface
{
    public function __construct(
        private readonly KeycloakUserPortInterface $userPort
    ) {
    }

    public function execute(CreateUserDTO $dto): UserDTO
    {
        $this->validateInput($dto);

        return $this->userPort->createUser($dto);
    }

    private function validateInput(CreateUserDTO $dto): void
    {
        $errors = [];

        if ($this->isEmpty($dto->username)) {
            $errors['username'] = 'O campo username é obrigatório.';
        }

        if ($this->isEmpty($dto->email)) {
            $errors['email'] = 'O campo email é obrigatório.';
        } elseif (!filter_var($dto->email, FILTER_VALIDATE_EMAIL)) {
            $errors['email'] = 'O campo email deve conter um endereço de e-mail válido.';
        }

        if ($this->isEmpty($dto->firstName)) {
            $errors['firstName'] = 'O campo firstName é obrigatório.';
        }

        if ($this->isEmpty($dto->lastName)) {
            $errors['lastName'] = 'O campo lastName é obrigatório.';
        }

        if ($this->isEmpty($dto->password)) {
            $errors['password'] = 'O campo password é obrigatório.';
        }

        if (!empty($errors)) {
            throw new ValidationException('Dados de usuário inválidos ou campos obrigatórios ausentes.', $errors);
        }
    }

    private function isEmpty(?string $value): bool
    {
        return $value === null || trim($value) === '';
    }
}
