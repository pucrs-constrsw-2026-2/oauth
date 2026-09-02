<?php

declare(strict_types=1);

namespace App\Domain\Exception;

class InvalidTokenException extends DomainException
{
    public function __construct(string $message = 'Token ausente, inválido ou expirado.')
    {
        parent::__construct($message, 401);
    }

    public function getErrorCode(): string
    {
        return 'INVALID_TOKEN';
    }

    public function getHttpStatusCode(): int
    {
        return 401;
    }
}
