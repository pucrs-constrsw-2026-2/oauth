<?php

declare(strict_types=1);

namespace App\Tests\Integration\Infrastructure\Http\Controller;

use Symfony\Bundle\FrameworkBundle\KernelBrowser;
use Symfony\Bundle\FrameworkBundle\Test\WebTestCase;
use Symfony\Component\HttpFoundation\Response;

final class SwaggerControllerTest extends WebTestCase
{
    private KernelBrowser $client;

    protected function setUp(): void
    {
        $this->client = static::createClient();
    }

    public function testOpenApiJsonReturnsValidSpecification(): void
    {
        $this->client->request('GET', '/docs/openapi.json');

        $response = $this->client->getResponse();
        $this->assertSame(Response::HTTP_OK, $response->getStatusCode());
        $this->assertStringContainsString('application/json', (string) $response->headers->get('Content-Type'));

        $data = json_decode((string) $response->getContent(), true);
        $this->assertSame('3.0.3', $data['openapi']);
        $this->assertSame('OAuth & OIDC Microservice API', $data['info']['title']);

        // Valida rotas essenciais no JSON
        $this->assertArrayHasKey('/login', $data['paths']);
        $this->assertArrayHasKey('/refresh', $data['paths']);
        $this->assertArrayHasKey('/me', $data['paths']);
        $this->assertArrayHasKey('/users', $data['paths']);
        $this->assertArrayHasKey('/users/{id}', $data['paths']);
        $this->assertArrayHasKey('/authorize', $data['paths']);
        $this->assertArrayHasKey('/health', $data['paths']);

        // Valida schemas
        $this->assertArrayHasKey('AuthorizeResponse', $data['components']['schemas']);
        $this->assertArrayHasKey('LoginRequest', $data['components']['schemas']);
    }

    public function testDocsUiReturnsStandardSwaggerUi(): void
    {
        $this->client->request('GET', '/docs');

        $response = $this->client->getResponse();
        $this->assertSame(Response::HTTP_OK, $response->getStatusCode());
        $this->assertStringContainsString('text/html', (string) $response->headers->get('Content-Type'));

        $html = (string) $response->getContent();
        $this->assertStringContainsString('Swagger UI', $html);
        $this->assertStringContainsString('swagger-ui', $html);
        $this->assertStringContainsString('SwaggerUIBundle', $html);
        $this->assertStringContainsString('StandaloneLayout', $html);
        $this->assertStringContainsString('/docs/openapi.json', $html);
    }

    public function testRemovedAliasesReturnNotFound(): void
    {
        $removedAliases = [
            '/docs.json',
            '/openapi.json',
            '/api/docs',
            '/swagger',
            '/docs/swagger',
            '/docs/scalar',
        ];

        foreach ($removedAliases as $alias) {
            $this->client->request('GET', $alias);
            $this->assertSame(
                Response::HTTP_NOT_FOUND,
                $this->client->getResponse()->getStatusCode(),
                "Alias {$alias} deveria retornar 404 Not Found"
            );
        }
    }
}
