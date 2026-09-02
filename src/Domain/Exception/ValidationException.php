<?php

declare(strict_types=1);

namespace App\Domain\Exception;

class ValidationException extends DomainException
{
    public function __construct(
        string $message = 'Dados inválidos ou campos obrigatórios ausentes.',
        private array $errors = []
    ) {
        parent::__construct($message, 400);
    }

    public function getErrorCode(): string
    {
        return 'VALIDATION_ERROR';
    }

    public function getHttpStatusCode(): int
    {
        return 400;
    }

    public function getErrors(): array
    {
        return $this->errors;
    }
}
