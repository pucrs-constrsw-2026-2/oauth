<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

use App\Application\DTO\Auth\RefreshTokenRequestDTO;
use App\Application\DTO\Auth\TokenResponseDTO;

interface RefreshTokenUseCaseInterface
{
    public function execute(RefreshTokenRequestDTO $dto): TokenResponseDTO;
}
