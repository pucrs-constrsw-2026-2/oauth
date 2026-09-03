<?php

declare(strict_types=1);

namespace App\Infrastructure\Keycloak\Client;

use App\Domain\Exception\InvalidTokenException;
use App\Domain\Exception\UserAlreadyExistsException;
use App\Domain\Exception\UserNotFoundException;
use RuntimeException;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class KeycloakHttpClient
{
    private ?string $cachedAdminToken = null;
    private int $adminTokenExpiresAt = 0;

    public function __construct(
        private HttpClientInterface $httpClient,
        private string $baseUrl,
        private string $realm = 'constrsw',
        private string $clientId = 'oauth',
        private string $clientSecret = 'a12345678-secret-mock'
    ) {
        $this->baseUrl = rtrim($this->baseUrl, '/');
    }

    public function getBaseUrl(): string
    {
        return $this->baseUrl;
    }

    public function getRealm(): string
    {
        return $this->realm;
    }

    public function getClientId(): string
    {
        return $this->clientId;
    }

    public function getClientSecret(): string
    {
        return $this->clientSecret;
    }

    /**
     * Executa uma requisição HTTP genérica para o Keycloak usando Symfony HttpClient
     */
    public function request(string $method, string $path, array $headers = [], ?string $body = null): array
    {
        $url = $this->baseUrl . '/' . ltrim($path, '/');

        $options = [
            'headers' => $headers,
            'timeout' => 15,
        ];

        if ($body !== null) {
            $options['body'] = $body;
        }

        $response = $this->httpClient->request(strtoupper($method), $url, $options);
        $statusCode = $response->getStatusCode();
        $headers = $response->getHeaders(false);
        $raw = $response->getContent(false);
        $data = json_decode($raw, true);

        return [
            'status' => $statusCode,
            'headers' => $headers,
            'data' => is_array($data) ? $data : [],
            'raw' => $raw,
        ];
    }

    /**
     * Obtém ou reutiliza token de serviço (Client Credentials) para a Admin API
     */
    public function getAdminToken(): string
    {
        $now = time();
        if ($this->cachedAdminToken !== null && $now < ($this->adminTokenExpiresAt - 30)) {
            return $this->cachedAdminToken;
        }

        $tokenUrl = "realms/{$this->realm}/protocol/openid-connect/token";
        $body = [
            'grant_type' => 'client_credentials',
            'client_id' => $this->clientId,
            'client_secret' => $this->clientSecret,
        ];

        $response = $this->httpClient->request('POST', $this->baseUrl . '/' . $tokenUrl, [
            'body' => $body,
            'timeout' => 15,
        ]);

        $statusCode = $response->getStatusCode();
        $raw = $response->getContent(false);
        $data = json_decode($raw, true);

        if ($statusCode !== 200 || empty($data['access_token'])) {
            throw new RuntimeException("Falha ao obter token de serviço no Keycloak. Status: {$statusCode}. Resposta: {$raw}");
        }

        $this->cachedAdminToken = (string) $data['access_token'];
        $expiresIn = (int) ($data['expires_in'] ?? 300);
        $this->adminTokenExpiresAt = $now + $expiresIn;

        return $this->cachedAdminToken;
    }

    /**
     * Executa requisição na Keycloak Admin API com Bearer token de serviço injetado
     */
    public function requestAdmin(string $method, string $path, array $headers = [], ?string $body = null): array
    {
        $token = $this->getAdminToken();
        $headers['Authorization'] = "Bearer {$token}";
        if (!isset($headers['Content-Type']) && in_array(strtoupper($method), ['POST', 'PUT', 'PATCH'], true)) {
            $headers['Content-Type'] = 'application/json';
        }

        $response = $this->request($method, $path, $headers, $body);

        // Mapeia erros conhecidos da Admin API para exceções de domínio
        if ($response['status'] === 404) {
            throw new UserNotFoundException();
        }

        if ($response['status'] === 409) {
            throw new UserAlreadyExistsException();
        }

        if ($response['status'] === 401) {
            throw new InvalidTokenException('Token de serviço expirado ou sem permissão na Admin API.');
        }

        return $response;
    }
}
