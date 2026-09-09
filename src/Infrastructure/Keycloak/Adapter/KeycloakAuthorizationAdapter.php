<?php

declare(strict_types=1);

namespace App\Infrastructure\Keycloak\Adapter;

use App\Application\DTO\Authorization\AuthorizeRequestDTO;
use App\Application\DTO\Authorization\AuthorizeResponseDTO;
use App\Domain\Exception\InvalidTokenException;
use App\Domain\Port\Outbound\KeycloakAuthorizationPortInterface;
use App\Infrastructure\Keycloak\Client\KeycloakHttpClient;

final class KeycloakAuthorizationAdapter implements KeycloakAuthorizationPortInterface
{
    /**
     * Matriz de papéis versus recursos conforme especificação do professor:
     * - administrator: resources, rooms, professors, students
     * - coordinator: courses, classes
     * - professor: lessons, reservations
     */
    private const RESOURCE_ROLES = [
        'resources' => ['administrator'],
        'rooms' => ['administrator'],
        'professors' => ['administrator'],
        'students' => ['administrator'],
        'courses' => ['coordinator'],
        'classes' => ['coordinator'],
        'lessons' => ['professor'],
        'reservations' => ['professor'],
    ];

    public function __construct(
        private readonly KeycloakHttpClient $httpClient
    ) {
    }

    public function validateAccess(AuthorizeRequestDTO $dto): AuthorizeResponseDTO
    {
        $this->validateTokenWithKeycloak($dto->accessToken);

        $resource = strtolower(trim($dto->resource));
        $userRoles = $this->extractRolesFromToken($dto->accessToken);

        if (!array_key_exists($resource, self::RESOURCE_ROLES)) {
            return new AuthorizeResponseDTO(
                authorized: false,
                resource: $dto->resource,
                matchingRoles: [],
                reason: "Recurso '{$dto->resource}' desconhecido ou não configurado na matriz de políticas."
            );
        }

        $allowedRoles = self::RESOURCE_ROLES[$resource];
        $matchingRoles = array_values(array_intersect($userRoles, $allowedRoles));

        if (!empty($matchingRoles)) {
            return new AuthorizeResponseDTO(
                authorized: true,
                resource: $dto->resource,
                matchingRoles: $matchingRoles,
                reason: 'Acesso autorizado com base nos papéis institucionais.'
            );
        }

        return new AuthorizeResponseDTO(
            authorized: false,
            resource: $dto->resource,
            matchingRoles: [],
            reason: "O usuário não possui permissão para acessar o recurso '{$dto->resource}'."
        );
    }

    private function validateTokenWithKeycloak(string $accessToken): void
    {
        $path = "realms/{$this->httpClient->getRealm()}/protocol/openid-connect/userinfo";
        $response = $this->httpClient->request('GET', $path, [
            'Authorization' => "Bearer {$accessToken}",
        ]);

        if ($response['status'] !== 200) {
            throw new InvalidTokenException('Token de acesso ausente, inválido ou expirado.');
        }
    }

    private function extractRolesFromToken(string $jwt): array
    {
        $parts = explode('.', $jwt);
        if (count($parts) < 2) {
            return [];
        }

        $payload = base64_decode(strtr($parts[1], '-_', '+/'));
        if ($payload === false) {
            return [];
        }

        $data = json_decode($payload, true);
        if (!is_array($data)) {
            return [];
        }

        $roles = [];
        if (!empty($data['realm_access']['roles']) && is_array($data['realm_access']['roles'])) {
            $roles = array_merge($roles, $data['realm_access']['roles']);
        }

        if (!empty($data['resource_access']) && is_array($data['resource_access'])) {
            foreach ($data['resource_access'] as $clientAccess) {
                if (!empty($clientAccess['roles']) && is_array($clientAccess['roles'])) {
                    $roles = array_merge($roles, $clientAccess['roles']);
                }
            }
        }

        return array_values(array_unique($roles));
    }
}
