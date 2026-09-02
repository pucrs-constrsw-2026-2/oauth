<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\Auth;

use App\Application\DTO\Auth\UserProfileDTO;
use App\Application\UseCase\Auth\GetUserInfoUseCase;
use App\Domain\Exception\InvalidTokenException;
use App\Domain\Port\Outbound\KeycloakAuthPortInterface;
use PHPUnit\Framework\TestCase;

final class GetUserInfoUseCaseTest extends TestCase
{
    private KeycloakAuthPortInterface $authPort;
    private GetUserInfoUseCase $useCase;

    protected function setUp(): void
    {
        $this->authPort = $this->createMock(KeycloakAuthPortInterface::class);
        $this->useCase = new GetUserInfoUseCase($this->authPort);
    }

    /**
     * Testa o fluxo de sucesso: token válido retorna o perfil do usuário logado.
     */
    public function testExecuteSuccessReturnsUserProfile(): void
    {
        $accessToken = 'valid-jwt-access-token';

        $expectedProfile = new UserProfileDTO(
            sub: 'user-prof-seed-id',
            name: 'Professor Seed',
            preferredUsername: 'professor@constrsw.pucrs.br',
            email: 'professor@constrsw.pucrs.br',
            emailVerified: true
        );

        $this->authPort
            ->expects($this->once())
            ->method('getUserInfo')
            ->with($accessToken)
            ->willReturn($expectedProfile);

        $result = $this->useCase->execute($accessToken);

        $this->assertSame($expectedProfile, $result);
        $this->assertSame('user-prof-seed-id', $result->sub);
        $this->assertSame('Professor Seed', $result->name);
        $this->assertSame('professor@constrsw.pucrs.br', $result->email);
        $this->assertTrue($result->emailVerified);
    }

    /**
     * Testa validação de entrada: rejeita token de acesso vazio.
     */
    public function testExecuteFailsWhenTokenIsEmpty(): void
    {
        $this->authPort->expects($this->never())->method('getUserInfo');

        $this->expectException(InvalidTokenException::class);
        $this->expectExceptionMessage('Token de acesso ausente ou inválido.');

        $this->useCase->execute('   ');
    }

    /**
     * Testa propagação de erro: token inválido ou expirado lança InvalidTokenException.
     */
    public function testExecutePropagatesInvalidTokenException(): void
    {
        $accessToken = 'invalid-or-expired-token';

        $this->authPort
            ->expects($this->once())
            ->method('getUserInfo')
            ->with($accessToken)
            ->willThrowException(new InvalidTokenException('Token de acesso ausente, inválido ou expirado.'));

        $this->expectException(InvalidTokenException::class);
        $this->expectExceptionMessage('Token de acesso ausente, inválido ou expirado.');

        $this->useCase->execute($accessToken);
    }
}
