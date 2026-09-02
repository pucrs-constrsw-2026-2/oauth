<?php

declare(strict_types=1);

namespace App\Application\DTO\Auth;

final class RefreshTokenRequestDTO
{
    public function __construct(
        public readonly string $refreshToken
    ) {
    }

    public static function fromArray(array $data): self
    {
        return new self(
            refreshToken: (string) ($data['refresh_token'] ?? '')
        );
    }
}
