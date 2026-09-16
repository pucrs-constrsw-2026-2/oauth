<?php

declare(strict_types=1);

namespace App\Application\DTO\Role;

final class RoleDTO
{
    public function __construct(
        public readonly string $id,
        public readonly string $name,
        public readonly ?string $description = null,
        public readonly bool $enabled = true
    ) {
    }

    public function toArray(): array
    {
        return [
            'id' => $this->id,
            'name' => $this->name,
            'description' => $this->description,
            'enabled' => $this->enabled,
        ];
    }
}
