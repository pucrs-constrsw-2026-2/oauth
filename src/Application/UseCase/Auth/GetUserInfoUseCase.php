<?php

declare(strict_types=1);

namespace App\Application\UseCase\Auth;

use App\Application\DTO\Auth\UserProfileDTO;
use App\Domain\Exception\InvalidTokenException;
use App\Domain\Port\Inbound\GetUserInfoUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakAuthPortInterface;

final class GetUserInfoUseCase implements GetUserInfoUseCaseInterface
{
    public function __construct(
        private readonly KeycloakAuthPortInterface $authPort
    ) {
    }

    public function execute(string $accessToken): UserProfileDTO
    {
        $this->validateToken($accessToken);

        return $this->authPort->getUserInfo($accessToken);
    }

    private function validateToken(string $accessToken): void
    {
        if ($this->isTokenEmpty($accessToken)) {
            throw new InvalidTokenException('Token de acesso ausente ou inválido.');
        }
    }

    private function isTokenEmpty(string $token): bool
    {
        return trim($token) === '';
    }
}
