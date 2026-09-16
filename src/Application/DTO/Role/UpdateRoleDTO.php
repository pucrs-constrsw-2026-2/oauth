<?php

declare(strict_types=1);

namespace App\Application\DTO\Role;

final class UpdateRoleDTO
{
    public function __construct(
        public readonly ?string $name = null,
        public readonly ?string $description = null
    ) {
    }

    public static function fromArray(array $data): self
    {
        return new self(
            name: isset($data['name']) ? (string) $data['name'] : null,
            description: isset($data['description']) ? (string) $data['description'] : null
        );
    }

    public function toArray(): array
    {
        $result = [];

        if ($this->name !== null) {
            $result['name'] = $this->name;
        }

        if ($this->description !== null) {
            $result['description'] = $this->description;
        }

        return $result;
    }
}
