<?php

declare(strict_types=1);

namespace App\Application\DTO\Auth;

final class TokenResponseDTO
{
    public function __construct(
        public readonly string $tokenType,
        public readonly string $accessToken,
        public readonly int $expiresIn,
        public readonly string $refreshToken,
        public readonly int $refreshExpiresIn
    ) {
    }

    public function toArray(): array
    {
        return [
            'token_type' => $this->tokenType,
            'access_token' => $this->accessToken,
            'expires_in' => $this->expiresIn,
            'refresh_token' => $this->refreshToken,
            'refresh_expires_in' => $this->refreshExpiresIn,
        ];
    }
}
