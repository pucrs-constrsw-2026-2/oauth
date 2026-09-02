<?php

declare(strict_types=1);

namespace App\Domain\Exception;

use Exception;

abstract class DomainException extends Exception
{
    abstract public function getErrorCode(): string;
    abstract public function getHttpStatusCode(): int;
}
