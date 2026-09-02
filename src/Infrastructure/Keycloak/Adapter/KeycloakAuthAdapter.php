<?php

declare(strict_types=1);

namespace App\Infrastructure\Keycloak\Adapter;

use App\Application\DTO\Auth\LoginRequestDTO;
use App\Application\DTO\Auth\RefreshTokenRequestDTO;
use App\Application\DTO\Auth\TokenResponseDTO;
use App\Application\DTO\Auth\UserProfileDTO;
use App\Domain\Exception\InvalidCredentialsException;
use App\Domain\Exception\InvalidTokenException;
use App\Domain\Port\Outbound\KeycloakAuthPortInterface;
use App\Infrastructure\Keycloak\Client\KeycloakHttpClient;
use RuntimeException;

final class KeycloakAuthAdapter implements KeycloakAuthPortInterface
{
    private const OIDC_SCOPE = 'openid profile email';

    public function __construct(
        private readonly KeycloakHttpClient $httpClient
    ) {
    }

    public function authenticate(LoginRequestDTO $dto): TokenResponseDTO
    {
        $payload = $this->buildAuthPayload($dto);
        $response = $this->requestToken($payload);

        return $this->handleAuthResponse($response);
    }

    public function refreshToken(RefreshTokenRequestDTO $dto): TokenResponseDTO
    {
        $payload = $this->buildRefreshPayload($dto);
        $response = $this->requestToken($payload);

        return $this->handleRefreshResponse($response);
    }

    public function getUserInfo(string $accessToken): UserProfileDTO
    {
        $response = $this->executeUserInfoRequest($accessToken);

        return $this->handleUserInfoResponse($response);
    }

    private function buildAuthPayload(LoginRequestDTO $dto): array
    {
        return [
            'grant_type' => 'password',
            'client_id' => $dto->clientId ?? $this->httpClient->getClientId(),
            'client_secret' => $this->httpClient->getClientSecret(),
            'username' => $dto->username,
            'password' => $dto->password,
            'scope' => self::OIDC_SCOPE,
        ];
    }

    private function buildRefreshPayload(RefreshTokenRequestDTO $dto): array
    {
        return [
            'grant_type' => 'refresh_token',
            'client_id' => $this->httpClient->getClientId(),
            'client_secret' => $this->httpClient->getClientSecret(),
            'refresh_token' => $dto->refreshToken,
        ];
    }

    private function requestToken(array $payload): array
    {
        return $this->httpClient->request(
            'POST',
            $this->tokenPath(),
            ['Content-Type' => 'application/x-www-form-urlencoded'],
            http_build_query($payload)
        );
    }

    private function executeUserInfoRequest(string $accessToken): array
    {
        return $this->httpClient->request(
            'GET',
            $this->userInfoPath(),
            ['Authorization' => "Bearer {$accessToken}"]
        );
    }

    private function handleAuthResponse(array $response): TokenResponseDTO
    {
        if ($this->isSuccessStatus($response['status'])) {
            return $this->createTokenResponse($response['data']);
        }

        $this->handleAuthFailure($response);
    }

    private function handleRefreshResponse(array $response): TokenResponseDTO
    {
        if ($this->isSuccessStatus($response['status'])) {
            return $this->createTokenResponse($response['data']);
        }

        $this->handleRefreshFailure($response);
    }

    private function handleUserInfoResponse(array $response): UserProfileDTO
    {
        if ($this->isSuccessStatus($response['status'])) {
            return $this->createUserProfile($response['data']);
        }

        $this->handleUserInfoFailure($response);
    }

    private function handleAuthFailure(array $response): never
    {
        if ($this->isClientAuthError($response['status'])) {
            $errorMessage = $this->extractErrorMessage($response['data']);
            throw new InvalidCredentialsException($errorMessage ?? 'Credenciais inválidas ou usuário desabilitado.');
        }

        throw new RuntimeException("Erro inesperado ao autenticar no Keycloak. Status: {$response['status']}");
    }

    private function handleRefreshFailure(array $response): never
    {
        if ($this->isClientAuthError($response['status'])) {
            throw new InvalidTokenException('Refresh token expirado, inválido ou revogado.');
        }

        throw new RuntimeException("Erro inesperado ao renovar token no Keycloak. Status: {$response['status']}");
    }

    private function handleUserInfoFailure(array $response): never
    {
        if ($this->isTokenVerificationError($response['status'])) {
            throw new InvalidTokenException('Token de acesso ausente, inválido ou expirado.');
        }

        throw new RuntimeException("Erro inesperado ao obter dados do usuário no Keycloak. Status: {$response['status']}");
    }

    private function createTokenResponse(array $data): TokenResponseDTO
    {
        return new TokenResponseDTO(
            tokenType: (string) ($data['token_type'] ?? 'Bearer'),
            accessToken: (string) ($data['access_token'] ?? ''),
            expiresIn: (int) ($data['expires_in'] ?? 300),
            refreshToken: (string) ($data['refresh_token'] ?? ''),
            refreshExpiresIn: (int) ($data['refresh_expires_in'] ?? 1800)
        );
    }

    private function createUserProfile(array $data): UserProfileDTO
    {
        return new UserProfileDTO(
            sub: (string) ($data['sub'] ?? ''),
            name: (string) ($data['name'] ?? ''),
            preferredUsername: (string) ($data['preferred_username'] ?? ''),
            email: (string) ($data['email'] ?? ''),
            emailVerified: (bool) ($data['email_verified'] ?? false),
            rawClaims: $data
        );
    }

    private function isSuccessStatus(int $status): bool
    {
        return $status >= 200 && $status < 300;
    }

    private function isClientAuthError(int $status): bool
    {
        return $status === 400 || $status === 401;
    }

    private function isTokenVerificationError(int $status): bool
    {
        return $status === 401 || $status === 403;
    }

    private function extractErrorMessage(array $data): ?string
    {
        return $data['error_description'] ?? $data['error'] ?? null;
    }

    private function tokenPath(): string
    {
        return "realms/{$this->httpClient->getRealm()}/protocol/openid-connect/token";
    }

    private function userInfoPath(): string
    {
        return "realms/{$this->httpClient->getRealm()}/protocol/openid-connect/userinfo";
    }
}
