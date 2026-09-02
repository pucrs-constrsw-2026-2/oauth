<?php

declare(strict_types=1);

namespace App\Domain\Exception;

class UserAlreadyExistsException extends DomainException
{
    public function __construct(string $message = 'Já existe um usuário com o e-mail informado.')
    {
        parent::__construct($message, 409);
    }

    public function getErrorCode(): string
    {
        return 'USER_ALREADY_EXISTS';
    }

    public function getHttpStatusCode(): int
    {
        return 409;
    }
}
