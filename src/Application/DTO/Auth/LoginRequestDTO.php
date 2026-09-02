<?php

declare(strict_types=1);

namespace App\Application\DTO\Auth;

final class LoginRequestDTO
{
    public function __construct(
        public readonly string $username,
        public readonly string $password,
        public readonly ?string $clientId = null
    ) {
    }

    public static function fromArray(array $data): self
    {
        return new self(
            username: (string) ($data['username'] ?? ''),
            password: (string) ($data['password'] ?? ''),
            clientId: isset($data['client_id']) ? (string) $data['client_id'] : null
        );
    }
}
