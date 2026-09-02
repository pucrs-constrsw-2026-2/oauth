<?php

declare(strict_types=1);

namespace App\Application\DTO\Auth;

final class UserProfileDTO
{
    public function __construct(
        public readonly string $sub,
        public readonly string $name,
        public readonly string $preferredUsername,
        public readonly string $email,
        public readonly bool $emailVerified = false,
        public readonly array $rawClaims = []
    ) {
    }

    public function toArray(): array
    {
        return [
            'sub' => $this->sub,
            'name' => $this->name,
            'preferred_username' => $this->preferredUsername,
            'email' => $this->email,
            'email_verified' => $this->emailVerified,
        ];
    }
}
