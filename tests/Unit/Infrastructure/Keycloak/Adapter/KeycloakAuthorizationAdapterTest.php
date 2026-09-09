<?php

declare(strict_types=1);

namespace App\Tests\Unit\Infrastructure\Keycloak\Adapter;

use App\Application\DTO\Authorization\AuthorizeRequestDTO;
use App\Domain\Exception\InvalidTokenException;
use App\Infrastructure\Keycloak\Adapter\KeycloakAuthorizationAdapter;
use App\Infrastructure\Keycloak\Client\KeycloakHttpClient;
use PHPUnit\Framework\TestCase;

final class KeycloakAuthorizationAdapterTest extends TestCase
{
    private KeycloakHttpClient $httpClient;
    private KeycloakAuthorizationAdapter $adapter;

    protected function setUp(): void
    {
        $this->httpClient = $this->createMock(KeycloakHttpClient::class);
        $this->httpClient->method('getRealm')->willReturn('constrsw');
        $this->adapter = new KeycloakAuthorizationAdapter($this->httpClient);
    }

    public function testValidateAccessAuthorizedForProfessorOnLessons(): void
    {
        $jwt = $this->createMockJwt(['professor', 'default-roles-construcao-sw']);
        $dto = new AuthorizeRequestDTO($jwt, 'lessons');

        $this->httpClient
            ->expects($this->once())
            ->method('request')
            ->with('GET', 'realms/constrsw/protocol/openid-connect/userinfo', ['Authorization' => "Bearer {$jwt}"])
            ->willReturn(['status' => 200, 'data' => ['sub' => 'prof-id'], 'raw' => '']);

        $response = $this->adapter->validateAccess($dto);

        $this->assertTrue($response->authorized);
        $this->assertSame('lessons', $response->resource);
        $this->assertSame(['professor'], $response->matchingRoles);
    }

    public function testValidateAccessAuthorizedForAdministratorOnRooms(): void
    {
        $jwt = $this->createMockJwt(['administrator']);
        $dto = new AuthorizeRequestDTO($jwt, 'rooms');

        $this->httpClient
            ->expects($this->once())
            ->method('request')
            ->willReturn(['status' => 200, 'data' => ['sub' => 'admin-id'], 'raw' => '']);

        $response = $this->adapter->validateAccess($dto);

        $this->assertTrue($response->authorized);
        $this->assertSame('rooms', $response->resource);
        $this->assertSame(['administrator'], $response->matchingRoles);
    }

    public function testValidateAccessAuthorizedForCoordinatorOnCourses(): void
    {
        $jwt = $this->createMockJwt(['coordinator']);
        $dto = new AuthorizeRequestDTO($jwt, 'courses');

        $this->httpClient
            ->expects($this->once())
            ->method('request')
            ->willReturn(['status' => 200, 'data' => ['sub' => 'coord-id'], 'raw' => '']);

        $response = $this->adapter->validateAccess($dto);

        $this->assertTrue($response->authorized);
        $this->assertSame('courses', $response->resource);
        $this->assertSame(['coordinator'], $response->matchingRoles);
    }

    public function testValidateAccessDeniedForProfessorOnClasses(): void
    {
        $jwt = $this->createMockJwt(['professor']);
        $dto = new AuthorizeRequestDTO($jwt, 'classes');

        $this->httpClient
            ->expects($this->once())
            ->method('request')
            ->willReturn(['status' => 200, 'data' => ['sub' => 'prof-id'], 'raw' => '']);

        $response = $this->adapter->validateAccess($dto);

        $this->assertFalse($response->authorized);
        $this->assertSame('classes', $response->resource);
        $this->assertEmpty($response->matchingRoles);
    }

    public function testValidateAccessDeniedForStudentOnRooms(): void
    {
        $jwt = $this->createMockJwt(['student']);
        $dto = new AuthorizeRequestDTO($jwt, 'rooms');

        $this->httpClient
            ->expects($this->once())
            ->method('request')
            ->willReturn(['status' => 200, 'data' => ['sub' => 'student-id'], 'raw' => '']);

        $response = $this->adapter->validateAccess($dto);

        $this->assertFalse($response->authorized);
        $this->assertSame('rooms', $response->resource);
        $this->assertEmpty($response->matchingRoles);
    }

    public function testValidateAccessDeniedForUnknownResource(): void
    {
        $jwt = $this->createMockJwt(['administrator']);
        $dto = new AuthorizeRequestDTO($jwt, 'unknown_resource_xyz');

        $this->httpClient
            ->expects($this->once())
            ->method('request')
            ->willReturn(['status' => 200, 'data' => ['sub' => 'admin-id'], 'raw' => '']);

        $response = $this->adapter->validateAccess($dto);

        $this->assertFalse($response->authorized);
        $this->assertSame('unknown_resource_xyz', $response->resource);
    }

    public function testValidateAccessThrowsInvalidTokenExceptionWhenKeycloakRejectsToken(): void
    {
        $jwt = $this->createMockJwt(['professor']);
        $dto = new AuthorizeRequestDTO($jwt, 'lessons');

        $this->httpClient
            ->expects($this->once())
            ->method('request')
            ->willReturn(['status' => 401, 'data' => ['error' => 'invalid_token'], 'raw' => '']);

        $this->expectException(InvalidTokenException::class);
        $this->expectExceptionCode(401);

        $this->adapter->validateAccess($dto);
    }

    private function createMockJwt(array $roles): string
    {
        $header = base64_encode(json_encode(['alg' => 'RS256', 'typ' => 'JWT']));
        $payload = base64_encode(json_encode([
            'sub' => 'user-sub-uuid',
            'realm_access' => ['roles' => $roles],
        ]));

        return "{$header}.{$payload}.fake_signature";
    }
}
