<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\User;

use App\Application\UseCase\User\DisableUserUseCase;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Outbound\KeycloakUserPortInterface;
use PHPUnit\Framework\TestCase;

final class DisableUserUseCaseTest extends TestCase
{
    private KeycloakUserPortInterface $userPort;
    private DisableUserUseCase $useCase;

    protected function setUp(): void
    {
        $this->userPort = $this->createMock(KeycloakUserPortInterface::class);
        $this->useCase = new DisableUserUseCase($this->userPort);
    }

    public function testExecuteSuccessDisablesUser(): void
    {
        $this->userPort
            ->expects($this->once())
            ->method('disableUser')
            ->with('uuid-target');

        $this->useCase->execute('uuid-target');
    }

    public function testExecuteThrowsValidationExceptionWhenIdIsEmpty(): void
    {
        $this->userPort->expects($this->never())->method('disableUser');

        $this->expectException(ValidationException::class);
        $this->useCase->execute('');
    }
}
