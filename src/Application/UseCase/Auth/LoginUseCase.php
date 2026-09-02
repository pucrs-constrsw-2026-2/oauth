<?php

declare(strict_types=1);

namespace App\Application\UseCase\Auth;

use App\Application\DTO\Auth\LoginRequestDTO;
use App\Application\DTO\Auth\TokenResponseDTO;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Inbound\LoginUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakAuthPortInterface;

final class LoginUseCase implements LoginUseCaseInterface
{
    public function __construct(
        private readonly KeycloakAuthPortInterface $authPort
    ) {
    }

    public function execute(LoginRequestDTO $dto): TokenResponseDTO
    {
        $this->validateInput($dto);

        return $this->authPort->authenticate($dto);
    }

    private function validateInput(LoginRequestDTO $dto): void
    {
        $errors = $this->collectValidationErrors($dto);

        if (!empty($errors)) {
            throw new ValidationException('Credenciais de login inválidas ou campos obrigatórios ausentes.', $errors);
        }
    }

    private function collectValidationErrors(LoginRequestDTO $dto): array
    {
        $errors = [];

        if ($this->isFieldEmpty($dto->username)) {
            $errors['username'] = 'O campo username é obrigatório.';
        }

        if ($this->isFieldEmpty($dto->password)) {
            $errors['password'] = 'O campo password é obrigatório.';
        }

        return $errors;
    }

    private function isFieldEmpty(?string $value): bool
    {
        return $value === null || trim($value) === '';
    }
}
