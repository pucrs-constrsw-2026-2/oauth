<?php

declare(strict_types=1);

namespace App\Domain\Exception;

class AccessDeniedException extends DomainException
{
    public function __construct(string $message = 'Acesso negado ao recurso solicitado.')
    {
        parent::__construct($message, 403);
    }

    public function getErrorCode(): string
    {
        return 'ACCESS_DENIED';
    }

    public function getHttpStatusCode(): int
    {
        return 403;
    }
}
