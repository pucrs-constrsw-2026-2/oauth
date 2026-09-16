<?php

declare(strict_types=1);

namespace App\Application\DTO\Role;

final class CreateRoleDTO
{
    public function __construct(
        public readonly string $name,
        public readonly ?string $description = null
    ) {
    }

    public static function fromArray(array $data): self
    {
        return new self(
            name: trim((string) ($data['name'] ?? '')),
            description: isset($data['description']) && $data['description'] !== null ? (string) $data['description'] : null
        );
    }
}
