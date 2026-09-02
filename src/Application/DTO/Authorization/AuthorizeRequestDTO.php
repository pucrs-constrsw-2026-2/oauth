<?php

declare(strict_types=1);

namespace App\Application\DTO\Authorization;

final class AuthorizeRequestDTO
{
    public function __construct(
        public readonly string $accessToken,
        public readonly string $resource
    ) {
    }

    public static function fromRequest(string $accessToken, array $body): self
    {
        return new self(
            accessToken: $accessToken,
            resource: (string) ($body['resource'] ?? '')
        );
    }
}
