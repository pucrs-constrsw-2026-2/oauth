<?php

declare(strict_types=1);

namespace App\Application\DTO\Role;

final class AssignRoleDTO
{
    public function __construct(
        public readonly ?string $roleId = null,
        public readonly ?string $name = null
    ) {
    }

    public static function fromArray(array $data): self
    {
        $name = isset($data['name']) && trim((string) $data['name']) !== ''
            ? trim((string) $data['name'])
            : (isset($data['roleName']) && trim((string) $data['roleName']) !== '' ? trim((string) $data['roleName']) : null);

        return new self(
            roleId: isset($data['roleId']) && trim((string) $data['roleId']) !== '' ? trim((string) $data['roleId']) : null,
            name: $name
        );
    }

    public function getRoleIdentifier(): ?string
    {
        return $this->roleId ?? $this->name;
    }

    public function toArray(): array
    {
        $result = [];

        if ($this->roleId !== null) {
            $result['roleId'] = $this->roleId;
        }

        if ($this->name !== null) {
            $result['name'] = $this->name;
        }

        return $result;
    }
}
