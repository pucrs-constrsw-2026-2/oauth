<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

use App\Application\DTO\Authorization\AuthorizeRequestDTO;
use App\Application\DTO\Authorization\AuthorizeResponseDTO;

interface AuthorizeResourceUseCaseInterface
{
    public function execute(AuthorizeRequestDTO $dto): AuthorizeResponseDTO;
}
