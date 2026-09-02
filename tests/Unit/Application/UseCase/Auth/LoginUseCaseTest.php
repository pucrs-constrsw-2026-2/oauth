<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\Auth;

use App\Application\DTO\Auth\LoginRequestDTO;
use App\Application\DTO\Auth\TokenResponseDTO;
use App\Application\UseCase\Auth\LoginUseCase;
use App\Domain\Exception\InvalidCredentialsException;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Outbound\KeycloakAuthPortInterface;
use PHPUnit\Framework\TestCase;

final class LoginUseCaseTest extends TestCase
{
    private KeycloakAuthPortInterface $authPort;
    private LoginUseCase $useCase;

    protected function setUp(): void
    {
        $this->authPort = $this->createMock(KeycloakAuthPortInterface::class);
        $this->useCase = new LoginUseCase($this->authPort);
    }

    /**
     * Testa o fluxo de sucesso: credenciais válidas geram os tokens esperados.
     */
    public function testExecuteSuccessReturnsTokens(): void
    {
        $dto = new LoginRequestDTO(
            username: 'user@example.com',
            password: 'secretPassword123'
        );

        $expectedTokens = new TokenResponseDTO(
            tokenType: 'Bearer',
            accessToken: 'sample-access-token',
            expiresIn: 300,
            refreshToken: 'sample-refresh-token',
            refreshExpiresIn: 1800
        );

        $this->authPort
            ->expects($this->once())
            ->method('authenticate')
            ->with($dto)
            ->willReturn($expectedTokens);

        $result = $this->useCase->execute($dto);

        $this->assertSame($expectedTokens, $result);
        $this->assertSame('sample-access-token', $result->accessToken);
        $this->assertSame('sample-refresh-token', $result->refreshToken);
    }

    /**
     * Testa validação de entrada: rejeita requisição sem username.
     */
    public function testExecuteFailsWhenUsernameIsEmpty(): void
    {
        $dto = new LoginRequestDTO(username: '', password: 'secretPassword123');

        $this->authPort->expects($this->never())->method('authenticate');

        $this->expectException(ValidationException::class);

        try {
            $this->useCase->execute($dto);
        } catch (ValidationException $e) {
            $this->assertSame('VALIDATION_ERROR', $e->getErrorCode());
            $this->assertArrayHasKey('username', $e->getErrors());
            $this->assertArrayNotHasKey('password', $e->getErrors());
            throw $e;
        }
    }

    /**
     * Testa validação de entrada: rejeita requisição sem senha.
     */
    public function testExecuteFailsWhenPasswordIsEmpty(): void
    {
        $dto = new LoginRequestDTO(username: 'user@example.com', password: '   ');

        $this->authPort->expects($this->never())->method('authenticate');

        $this->expectException(ValidationException::class);

        try {
            $this->useCase->execute($dto);
        } catch (ValidationException $e) {
            $this->assertSame('VALIDATION_ERROR', $e->getErrorCode());
            $this->assertArrayHasKey('password', $e->getErrors());
            $this->assertArrayNotHasKey('username', $e->getErrors());
            throw $e;
        }
    }

    /**
     * Testa validação de entrada: rejeita quando ambos os campos são omitidos.
     */
    public function testExecuteFailsWhenBothFieldsAreEmpty(): void
    {
        $dto = new LoginRequestDTO(username: '', password: '');

        $this->authPort->expects($this->never())->method('authenticate');

        $this->expectException(ValidationException::class);

        try {
            $this->useCase->execute($dto);
        } catch (ValidationException $e) {
            $this->assertSame('VALIDATION_ERROR', $e->getErrorCode());
            $this->assertArrayHasKey('username', $e->getErrors());
            $this->assertArrayHasKey('password', $e->getErrors());
            throw $e;
        }
    }

    /**
     * Testa propagação de erro: credenciais inválidas lançadas pela porta são repassadas.
     */
    public function testExecutePropagatesInvalidCredentialsException(): void
    {
        $dto = new LoginRequestDTO(
            username: 'user@example.com',
            password: 'wrongPassword'
        );

        $this->authPort
            ->expects($this->once())
            ->method('authenticate')
            ->with($dto)
            ->willThrowException(new InvalidCredentialsException('Invalid user credentials'));

        $this->expectException(InvalidCredentialsException::class);
        $this->expectExceptionMessage('Invalid user credentials');

        $this->useCase->execute($dto);
    }
}
