<?php

declare(strict_types=1);

namespace App\Domain\Exception;

class RoleAlreadyExistsException extends DomainException
{
    public function __construct(string $message = 'Já existe um role com o nome informado.')
    {
        parent::__construct($message, 409);
    }

    public function getErrorCode(): string
    {
        return 'ROLE_ALREADY_EXISTS';
    }

    public function getHttpStatusCode(): int
    {
        return 409;
    }
}
