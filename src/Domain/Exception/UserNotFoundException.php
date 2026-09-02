<?php

declare(strict_types=1);

namespace App\Domain\Exception;

class UserNotFoundException extends DomainException
{
    public function __construct(string $message = 'Usuário não encontrado.')
    {
        parent::__construct($message, 404);
    }

    public function getErrorCode(): string
    {
        return 'USER_NOT_FOUND';
    }

    public function getHttpStatusCode(): int
    {
        return 404;
    }
}
