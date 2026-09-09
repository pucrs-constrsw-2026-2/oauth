<?php

declare(strict_types=1);

namespace App\Tests\Unit\Infrastructure\OpenApi;

use App\Infrastructure\OpenApi\OpenApiSpecificationBuilder;
use PHPUnit\Framework\TestCase;
use Symfony\Component\Routing\Route;
use Symfony\Component\Routing\RouteCollection;
use Symfony\Component\Routing\RouterInterface;

final class OpenApiSpecificationBuilderTest extends TestCase
{
    public function testBuildContainsRequiredOpenApiMetadata(): void
    {
        $router = $this->createMock(RouterInterface::class);
        $router->method('getRouteCollection')->willReturn(new RouteCollection());

        $builder = new OpenApiSpecificationBuilder($router);
        $spec = $builder->build('http://localhost:8181');

        $this->assertSame('3.0.3', $spec['openapi']);
        $this->assertSame('OAuth & OIDC Microservice API', $spec['info']['title']);
        $this->assertSame('1.0.0', $spec['info']['version']);
        $this->assertArrayHasKey('servers', $spec);
        $this->assertSame('http://localhost:8181', $spec['servers'][0]['url']);
        $this->assertArrayHasKey('components', $spec);
        $this->assertArrayHasKey('securitySchemes', $spec['components']);
        $this->assertArrayHasKey('bearerAuth', $spec['components']['securitySchemes']);
        $this->assertSame('http', $spec['components']['securitySchemes']['bearerAuth']['type']);
        $this->assertSame('bearer', $spec['components']['securitySchemes']['bearerAuth']['scheme']);
        $this->assertSame('JWT', $spec['components']['securitySchemes']['bearerAuth']['bearerFormat']);
    }

    public function testBuildContainsPredefinedCoreEndpoints(): void
    {
        $router = $this->createMock(RouterInterface::class);
        $router->method('getRouteCollection')->willReturn(new RouteCollection());

        $builder = new OpenApiSpecificationBuilder($router);
        $spec = $builder->build();

        $paths = $spec['paths'];
        $this->assertArrayHasKey('/login', $paths);
        $this->assertArrayHasKey('post', $paths['login'] ?? $paths['/login']);
        $this->assertArrayHasKey('/refresh', $paths);
        $this->assertArrayHasKey('/me', $paths);
        $this->assertArrayHasKey('/users', $paths);
        $this->assertArrayHasKey('/users/{id}', $paths);
        $this->assertArrayHasKey('/authorize', $paths);
        $this->assertArrayHasKey('/health', $paths);

        // Valida endpoint de autorização (FR-10)
        $authzOp = $paths['/authorize']['post'];
        $this->assertSame(['Autorização'], $authzOp['tags']);
        $this->assertArrayHasKey('security', $authzOp);
        $this->assertArrayHasKey('200', $authzOp['responses']);
        $this->assertArrayHasKey('403', $authzOp['responses']);
    }

    public function testBuildContainsRequiredSchemas(): void
    {
        $router = $this->createMock(RouterInterface::class);
        $router->method('getRouteCollection')->willReturn(new RouteCollection());

        $builder = new OpenApiSpecificationBuilder($router);
        $spec = $builder->build();

        $schemas = $spec['components']['schemas'];
        $this->assertArrayHasKey('LoginRequest', $schemas);
        $this->assertArrayHasKey('TokenResponse', $schemas);
        $this->assertArrayHasKey('RefreshTokenRequest', $schemas);
        $this->assertArrayHasKey('UserProfileResponse', $schemas);
        $this->assertArrayHasKey('CreateUserRequest', $schemas);
        $this->assertArrayHasKey('UpdateUserRequest', $schemas);
        $this->assertArrayHasKey('UpdatePasswordRequest', $schemas);
        $this->assertArrayHasKey('UserResponse', $schemas);
        $this->assertArrayHasKey('AuthorizeRequest', $schemas);
        $this->assertArrayHasKey('AuthorizeResponse', $schemas);
        $this->assertArrayHasKey('HealthResponse', $schemas);
        $this->assertArrayHasKey('ErrorResponse', $schemas);
    }

    public function testBuildDiscoversDynamicRoutesAutomatically(): void
    {
        $routes = new RouteCollection();
        $routes->add('course_detail', new Route('/courses/{courseId}', [
            '_controller' => 'App\Infrastructure\Http\Controller\CourseController::getCourse',
        ], [], [], '', [], ['GET']));

        $router = $this->createMock(RouterInterface::class);
        $router->method('getRouteCollection')->willReturn($routes);

        $builder = new OpenApiSpecificationBuilder($router);
        $spec = $builder->build();

        $paths = $spec['paths'];
        $this->assertArrayHasKey('/courses/{courseId}', $paths);
        $this->assertArrayHasKey('get', $paths['/courses/{courseId}']);

        $operation = $paths['/courses/{courseId}']['get'];
        $this->assertSame(['Course'], $operation['tags']);
        $this->assertCount(1, $operation['parameters']);
        $this->assertSame('courseId', $operation['parameters'][0]['name']);
        $this->assertSame('path', $operation['parameters'][0]['in']);
        $this->assertTrue($operation['parameters'][0]['required']);
    }
}
