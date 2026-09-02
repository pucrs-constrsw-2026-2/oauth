<?php

declare(strict_types=1);

namespace App\Domain\Port\Outbound;

use App\Application\DTO\Authorization\AuthorizeRequestDTO;
use App\Application\DTO\Authorization\AuthorizeResponseDTO;

interface KeycloakAuthorizationPortInterface
{
    public function validateAccess(AuthorizeRequestDTO $dto): AuthorizeResponseDTO;
}
