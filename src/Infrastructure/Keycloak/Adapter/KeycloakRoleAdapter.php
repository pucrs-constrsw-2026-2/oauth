<?php

declare(strict_types=1);

namespace App\Infrastructure\Keycloak\Adapter;

use App\Application\DTO\Role\CreateRoleDTO;
use App\Application\DTO\Role\RoleDTO;
use App\Application\DTO\Role\UpdateRoleDTO;
use App\Domain\Exception\RoleAlreadyExistsException;
use App\Domain\Exception\RoleNotFoundException;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;
use App\Infrastructure\Keycloak\Client\KeycloakHttpClient;
use RuntimeException;

final class KeycloakRoleAdapter implements KeycloakRolePortInterface
{
    public function __construct(
        private readonly KeycloakHttpClient $httpClient
    ) {
    }

    public function createRole(CreateRoleDTO $dto): RoleDTO
    {
        $payload = [
            'name' => $dto->name,
            'description' => $dto->description ?? '',
            'attributes' => [
                'enabled' => ['true'],
            ],
        ];

        $response = $this->httpClient->requestAdmin(
            'POST',
            $this->rolesAdminPath(),
            [],
            json_encode($payload, JSON_THROW_ON_ERROR),
            false
        );

        if ($response['status'] === 409) {
            throw new RoleAlreadyExistsException("Já existe um role com o nome '{$dto->name}'.");
        }

        if ($response['status'] !== 201 && $response['status'] !== 200) {
            throw new RuntimeException("Falha ao criar role no Keycloak. Status: {$response['status']}.");
        }

        $roleData = $this->lookupRoleByName($dto->name);

        if ($roleData === null) {
            throw new RuntimeException("Falha ao recuperar dados da role recém-criada '{$dto->name}'.");
        }

        return $this->mapToDTO($roleData);
    }

    /**
     * @return RoleDTO[]
     */
    public function listActiveRoles(): array
    {
        $response = $this->httpClient->requestAdmin(
            'GET',
            $this->rolesAdminPath() . '?briefRepresentation=false',
            [],
            null,
            false
        );

        $roles = [];
        $data = is_array($response['data']) ? $response['data'] : [];

        foreach ($data as $roleData) {
            if (!is_array($roleData)) {
                continue;
            }

            if ($this->isRoleActive($roleData)) {
                $roles[] = $this->mapToDTO($roleData);
            }
        }

        return $roles;
    }

    public function getRoleById(string $id): ?RoleDTO
    {
        $roleData = $this->fetchRoleRaw($id);
        if ($roleData === null || !$this->isRoleActive($roleData)) {
            return null;
        }

        return $this->mapToDTO($roleData);
    }

    public function updateRole(string $id, UpdateRoleDTO $dto): RoleDTO
    {
        $roleData = $this->fetchRoleRaw($id);
        if ($roleData === null || !$this->isRoleActive($roleData)) {
            throw new RoleNotFoundException("Role '{$id}' não encontrado.");
        }

        $roleId = (string) ($roleData['id'] ?? $id);
        $name = $dto->name !== null && trim($dto->name) !== '' ? trim($dto->name) : (string) ($roleData['name'] ?? '');
        $description = $dto->description !== null ? $dto->description : ($roleData['description'] ?? null);

        $attributes = is_array($roleData['attributes'] ?? null) ? $roleData['attributes'] : [];
        $attributes['enabled'] = ['true'];

        $payload = array_merge($roleData, [
            'name' => $name,
            'description' => $description ?? '',
            'attributes' => $attributes,
        ]);

        $response = $this->httpClient->requestAdmin(
            'PUT',
            $this->rolesByIdAdminPath($roleId),
            [],
            json_encode($payload, JSON_THROW_ON_ERROR),
            false
        );

        if ($response['status'] === 404) {
            throw new RoleNotFoundException("Role '{$id}' não encontrado.");
        }

        if ($response['status'] === 409) {
            throw new RoleAlreadyExistsException("Já existe um role com o nome '{$name}'.");
        }

        if ($response['status'] !== 200 && $response['status'] !== 204) {
            throw new RuntimeException("Falha ao atualizar role no Keycloak. Status: {$response['status']}.");
        }

        return new RoleDTO(
            id: $roleId,
            name: $name,
            description: $description !== null && $description !== '' ? $description : null,
            enabled: true
        );
    }

    public function disableRole(string $id): void
    {
        $roleData = $this->fetchRoleRaw($id);
        if ($roleData === null || !$this->isRoleActive($roleData)) {
            throw new RoleNotFoundException("Role '{$id}' não encontrado.");
        }

        $roleId = (string) ($roleData['id'] ?? $id);
        $attributes = is_array($roleData['attributes'] ?? null) ? $roleData['attributes'] : [];
        $attributes['enabled'] = ['false'];

        $payload = array_merge($roleData, [
            'attributes' => $attributes,
        ]);

        $response = $this->httpClient->requestAdmin(
            'PUT',
            $this->rolesByIdAdminPath($roleId),
            [],
            json_encode($payload, JSON_THROW_ON_ERROR),
            false
        );

        if ($response['status'] === 404) {
            throw new RoleNotFoundException("Role '{$id}' não encontrado.");
        }

        if ($response['status'] !== 200 && $response['status'] !== 204) {
            throw new RuntimeException("Falha ao inativar role no Keycloak. Status: {$response['status']}.");
        }
    }

    private function fetchRoleRaw(string $id): ?array
    {
        $response = $this->httpClient->requestAdmin(
            'GET',
            $this->rolesByIdAdminPath($id),
            [],
            null,
            false
        );

        if ($response['status'] === 200 && is_array($response['data']) && !empty($response['data'])) {
            return $response['data'];
        }

        return null;
    }

    private function lookupRoleByName(string $name): ?array
    {
        $encodedName = rawurlencode($name);
        $response = $this->httpClient->requestAdmin(
            'GET',
            $this->rolesAdminPath() . '/' . $encodedName,
            [],
            null,
            false
        );

        if ($response['status'] === 200 && is_array($response['data']) && !empty($response['data'])) {
            return $response['data'];
        }

        return null;
    }

    private function isRoleActive(array $data): bool
    {
        $attributes = $data['attributes'] ?? [];
        if (!is_array($attributes)) {
            return true;
        }

        $enabledAttr = $attributes['enabled'] ?? null;
        if (is_array($enabledAttr) && isset($enabledAttr[0])) {
            return $enabledAttr[0] !== 'false';
        }

        if (is_string($enabledAttr)) {
            return $enabledAttr !== 'false';
        }

        return true;
    }

    private function mapToDTO(array $data): RoleDTO
    {
        return new RoleDTO(
            id: (string) ($data['id'] ?? ''),
            name: (string) ($data['name'] ?? ''),
            description: isset($data['description']) && $data['description'] !== '' ? (string) $data['description'] : null,
            enabled: $this->isRoleActive($data)
        );
    }

    private function rolesAdminPath(): string
    {
        return sprintf('admin/realms/%s/roles', $this->httpClient->getRealm());
    }

    private function rolesByIdAdminPath(string $id): string
    {
        return sprintf('admin/realms/%s/roles-by-id/%s', $this->httpClient->getRealm(), rawurlencode($id));
    }
}
