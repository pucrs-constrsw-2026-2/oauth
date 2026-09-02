<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

use App\Application\DTO\Auth\LoginRequestDTO;
use App\Application\DTO\Auth\TokenResponseDTO;

interface LoginUseCaseInterface
{
    public function execute(LoginRequestDTO $dto): TokenResponseDTO;
}
