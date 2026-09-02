<?php

declare(strict_types=1);

namespace App\Domain\Port\Outbound;

use App\Application\DTO\Auth\LoginRequestDTO;
use App\Application\DTO\Auth\RefreshTokenRequestDTO;
use App\Application\DTO\Auth\TokenResponseDTO;
use App\Application\DTO\Auth\UserProfileDTO;

interface KeycloakAuthPortInterface
{
    public function authenticate(LoginRequestDTO $dto): TokenResponseDTO;

    public function refreshToken(RefreshTokenRequestDTO $dto): TokenResponseDTO;

    public function getUserInfo(string $accessToken): UserProfileDTO;
}
