<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\User;

use App\Application\DTO\User\UpdatePasswordDTO;
use App\Application\UseCase\User\UpdatePasswordUseCase;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Outbound\KeycloakUserPortInterface;
use PHPUnit\Framework\TestCase;

final class UpdatePasswordUseCaseTest extends TestCase
{
    private KeycloakUserPortInterface $userPort;
    private UpdatePasswordUseCase $useCase;

    protected function setUp(): void
    {
        $this->userPort = $this->createMock(KeycloakUserPortInterface::class);
        $this->useCase = new UpdatePasswordUseCase($this->userPort);
    }

    public function testExecuteSuccessUpdatesPassword(): void
    {
        $dto = new UpdatePasswordDTO('newSecurePassword123');

        $this->userPort
            ->expects($this->once())
            ->method('updatePassword')
            ->with('uuid-123', $dto);

        $this->useCase->execute('uuid-123', $dto);
    }

    public function testExecuteThrowsValidationExceptionWhenIdIsEmpty(): void
    {
        $dto = new UpdatePasswordDTO('newPassword');

        $this->userPort->expects($this->never())->method('updatePassword');

        $this->expectException(ValidationException::class);
        $this->useCase->execute('', $dto);
    }

    public function testExecuteThrowsValidationExceptionWhenPasswordIsEmpty(): void
    {
        $dto = new UpdatePasswordDTO('   ');

        $this->userPort->expects($this->never())->method('updatePassword');

        $this->expectException(ValidationException::class);
        $this->useCase->execute('uuid-123', $dto);
    }
}
