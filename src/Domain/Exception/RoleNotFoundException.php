<?php

declare(strict_types=1);

namespace App\Domain\Exception;

class RoleNotFoundException extends DomainException
{
    public function __construct(string $message = 'Role não encontrado.')
    {
        parent::__construct($message, 404);
    }

    public function getErrorCode(): string
    {
        return 'ROLE_NOT_FOUND';
    }

    public function getHttpStatusCode(): int
    {
        return 404;
    }
}
