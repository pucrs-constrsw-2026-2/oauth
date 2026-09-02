<?php

declare(strict_types=1);

namespace App\Domain\Exception;

class InvalidCredentialsException extends DomainException
{
    public function __construct(string $message = 'Credenciais inválidas.')
    {
        parent::__construct($message, 401);
    }

    public function getErrorCode(): string
    {
        return 'INVALID_CREDENTIALS';
    }

    public function getHttpStatusCode(): int
    {
        return 401;
    }
}
