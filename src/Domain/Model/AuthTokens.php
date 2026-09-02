<?php

declare(strict_types=1);

namespace App\Domain\Model;

final class AuthTokens
{
    public function __construct(
        private string $accessToken,
        private int $expiresIn,
        private string $refreshToken,
        private int $refreshExpiresIn,
        private string $tokenType = 'Bearer',
        private ?string $scope = null
    ) {
    }

    public function getAccessToken(): string
    {
        return $this->accessToken;
    }

    public function getExpiresIn(): int
    {
        return $this->expiresIn;
    }

    public function getRefreshToken(): string
    {
        return $this->refreshToken;
    }

    public function getRefreshExpiresIn(): int
    {
        return $this->refreshExpiresIn;
    }

    public function getTokenType(): string
    {
        return $this->tokenType;
    }

    public function getScope(): ?string
    {
        return $this->scope;
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
