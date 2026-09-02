<?php

declare(strict_types=1);

namespace App\Domain\Port\Inbound;

use App\Application\DTO\Auth\UserProfileDTO;

interface GetUserInfoUseCaseInterface
{
    public function execute(string $accessToken): UserProfileDTO;
}
