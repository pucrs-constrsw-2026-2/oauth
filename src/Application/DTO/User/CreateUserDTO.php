<?php

declare(strict_types=1);

namespace App\Application\DTO\User;

final class CreateUserDTO
{
    public function __construct(
        public readonly string $username,
        public readonly string $email,
        public readonly string $firstName,
        public readonly string $lastName,
        public readonly string $password,
        public readonly bool $enabled = true,
        public readonly array $roles = []
    ) {
    }

    public static function fromArray(array $data): self
    {
        return new self(
            username: (string) ($data['username'] ?? $data['email'] ?? ''),
            email: (string) ($data['email'] ?? ''),
            firstName: (string) ($data['firstName'] ?? $data['first_name'] ?? ''),
            lastName: (string) ($data['lastName'] ?? $data['last_name'] ?? ''),
            password: (string) ($data['password'] ?? ''),
            enabled: (bool) ($data['enabled'] ?? true),
            roles: (array) ($data['roles'] ?? [])
        );
    }
}
