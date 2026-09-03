<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\User;

use App\Application\DTO\User\UpdateUserDTO;
use App\Application\UseCase\User\UpdateUserUseCase;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Outbound\KeycloakUserPortInterface;
use PHPUnit\Framework\TestCase;

final class UpdateUserUseCaseTest extends TestCase
{
    private KeycloakUserPortInterface $userPort;
    private UpdateUserUseCase $useCase;

    protected function setUp(): void
    {
        $this->userPort = $this->createMock(KeycloakUserPortInterface::class);
        $this->useCase = new UpdateUserUseCase($this->userPort);
    }

    public function testExecuteSuccessUpdatesUser(): void
    {
        $dto = new UpdateUserDTO(
            firstName: 'UpdatedFirst',
            lastName: 'UpdatedLast',
            email: 'updated@example.com'
        );

        $this->userPort
            ->expects($this->once())
            ->method('updateUser')
            ->with('uuid-123', $dto);

        $this->useCase->execute('uuid-123', $dto);
    }

    public function testExecuteThrowsValidationExceptionWhenIdIsEmpty(): void
    {
        $dto = new UpdateUserDTO(firstName: 'John');

        $this->userPort->expects($this->never())->method('updateUser');

        $this->expectException(ValidationException::class);
        $this->useCase->execute('', $dto);
    }

    public function testExecuteThrowsValidationExceptionWhenNoFieldsProvided(): void
    {
        $dto = new UpdateUserDTO(firstName: null, lastName: null, email: null);

        $this->userPort->expects($this->never())->method('updateUser');

        $this->expectException(ValidationException::class);
        $this->useCase->execute('uuid-123', $dto);
    }

    public function testExecuteThrowsValidationExceptionWhenEmailIsInvalid(): void
    {
        $dto = new UpdateUserDTO(email: 'invalid-email');

        $this->userPort->expects($this->never())->method('updateUser');

        $this->expectException(ValidationException::class);
        $this->useCase->execute('uuid-123', $dto);
    }
}
