<?php

declare(strict_types=1);

namespace App\Application\DTO\User;

final class UpdatePasswordDTO
{
    public function __construct(
        public readonly string $password
    ) {
    }

    public static function fromArray(array $data): self
    {
        return new self(
            password: (string) ($data['password'] ?? '')
        );
    }
}
