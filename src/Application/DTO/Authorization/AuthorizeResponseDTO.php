<?php

declare(strict_types=1);

namespace App\Application\DTO\Authorization;

final class AuthorizeResponseDTO
{
    public function __construct(
        public readonly bool $authorized,
        public readonly string $resource,
        public readonly array $matchingRoles = [],
        public readonly ?string $reason = null
    ) {
    }

    public function toArray(): array
    {
        return [
            'authorized' => $this->authorized,
            'resource' => $this->resource,
            'matching_roles' => $this->matchingRoles,
            'reason' => $this->reason,
        ];
    }
}
