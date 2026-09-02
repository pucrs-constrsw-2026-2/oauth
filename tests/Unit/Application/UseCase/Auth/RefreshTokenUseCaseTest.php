<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\Auth;

use App\Application\DTO\Auth\RefreshTokenRequestDTO;
use App\Application\DTO\Auth\TokenResponseDTO;
use App\Application\UseCase\Auth\RefreshTokenUseCase;
use App\Domain\Exception\InvalidTokenException;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Outbound\KeycloakAuthPortInterface;
use PHPUnit\Framework\TestCase;

final class RefreshTokenUseCaseTest extends TestCase
{
    private KeycloakAuthPortInterface $authPort;
    private RefreshTokenUseCase $useCase;

    protected function setUp(): void
    {
        $this->authPort = $this->createMock(KeycloakAuthPortInterface::class);
        $this->useCase = new RefreshTokenUseCase($this->authPort);
    }

    /**
     * Testa o fluxo de sucesso: refresh token válido gera um novo par de tokens.
     */
    public function testExecuteSuccessReturnsNewTokens(): void
    {
        $dto = new RefreshTokenRequestDTO(refreshToken: 'valid-refresh-token-xyz');

        $expectedTokens = new TokenResponseDTO(
            tokenType: 'Bearer',
            accessToken: 'new-access-token',
            expiresIn: 300,
            refreshToken: 'new-refresh-token',
            refreshExpiresIn: 1800
        );

        $this->authPort
            ->expects($this->once())
            ->method('refreshToken')
            ->with($dto)
            ->willReturn($expectedTokens);

        $result = $this->useCase->execute($dto);

        $this->assertSame($expectedTokens, $result);
        $this->assertSame('new-access-token', $result->accessToken);
        $this->assertSame('new-refresh-token', $result->refreshToken);
    }

    /**
     * Testa validação de entrada: rejeita payload com refresh token vazio.
     */
    public function testExecuteFailsWhenRefreshTokenIsEmpty(): void
    {
        $dto = new RefreshTokenRequestDTO(refreshToken: '   ');

        $this->authPort->expects($this->never())->method('refreshToken');

        $this->expectException(ValidationException::class);

        try {
            $this->useCase->execute($dto);
        } catch (ValidationException $e) {
            $this->assertSame('VALIDATION_ERROR', $e->getErrorCode());
            $this->assertArrayHasKey('refresh_token', $e->getErrors());
            throw $e;
        }
    }

    /**
     * Testa propagação de erro: refresh token expirado ou revogado lança InvalidTokenException.
     */
    public function testExecutePropagatesInvalidTokenException(): void
    {
        $dto = new RefreshTokenRequestDTO(refreshToken: 'expired-or-revoked-token');

        $this->authPort
            ->expects($this->once())
            ->method('refreshToken')
            ->with($dto)
            ->willThrowException(new InvalidTokenException('Refresh token expirado, inválido ou revogado.'));

        $this->expectException(InvalidTokenException::class);
        $this->expectExceptionMessage('Refresh token expirado, inválido ou revogado.');

        $this->useCase->execute($dto);
    }
}
