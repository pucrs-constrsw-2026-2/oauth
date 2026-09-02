<?php

declare(strict_types=1);

namespace App\Application\DTO\User;

final class UserDTO
{
    public function __construct(
        public readonly string $id,
        public readonly string $username,
        public readonly string $email,
        public readonly string $firstName,
        public readonly string $lastName,
        public readonly bool $enabled = true,
        public readonly array $roles = []
    ) {
    }

    public function toArray(): array
    {
        return [
            'id' => $this->id,
            'username' => $this->username,
            'email' => $this->email,
            'firstName' => $this->firstName,
            'lastName' => $this->lastName,
            'enabled' => $this->enabled,
            'roles' => $this->roles,
        ];
    }
}
