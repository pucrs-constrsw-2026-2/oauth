<?php

declare(strict_types=1);

namespace App\Application\UseCase\Auth;

use App\Application\DTO\Auth\RefreshTokenRequestDTO;
use App\Application\DTO\Auth\TokenResponseDTO;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Inbound\RefreshTokenUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakAuthPortInterface;

final class RefreshTokenUseCase implements RefreshTokenUseCaseInterface
{
    public function __construct(
        private readonly KeycloakAuthPortInterface $authPort
    ) {
    }

    public function execute(RefreshTokenRequestDTO $dto): TokenResponseDTO
    {
        $this->validateInput($dto);

        return $this->authPort->refreshToken($dto);
    }

    private function validateInput(RefreshTokenRequestDTO $dto): void
    {
        if ($this->isFieldEmpty($dto->refreshToken)) {
            throw new ValidationException('Refresh token é obrigatório.', [
                'refresh_token' => 'O campo refresh_token é obrigatório.',
            ]);
        }
    }

    private function isFieldEmpty(?string $value): bool
    {
        return $value === null || trim($value) === '';
    }
}
