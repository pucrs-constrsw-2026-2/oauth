<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\User;

use App\Application\DTO\User\CreateUserDTO;
use App\Application\DTO\User\UserDTO;
use App\Application\UseCase\User\CreateUserUseCase;
use App\Domain\Exception\UserAlreadyExistsException;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Outbound\KeycloakUserPortInterface;
use PHPUnit\Framework\TestCase;

final class CreateUserUseCaseTest extends TestCase
{
    private KeycloakUserPortInterface $userPort;
    private CreateUserUseCase $useCase;

    protected function setUp(): void
    {
        $this->userPort = $this->createMock(KeycloakUserPortInterface::class);
        $this->useCase = new CreateUserUseCase($this->userPort);
    }

    public function testExecuteSuccessReturnsCreatedUser(): void
    {
        $dto = new CreateUserDTO(
            username: 'novousuario@constrsw.pucrs.br',
            email: 'novousuario@constrsw.pucrs.br',
            firstName: 'Novo',
            lastName: 'Usuario',
            password: 'Password123!',
            enabled: true,
            roles: ['student']
        );

        $expectedUser = new UserDTO(
            id: 'uuid-12345',
            username: 'novousuario@constrsw.pucrs.br',
            email: 'novousuario@constrsw.pucrs.br',
            firstName: 'Novo',
            lastName: 'Usuario',
            enabled: true,
            roles: ['student']
        );

        $this->userPort
            ->expects($this->once())
            ->method('createUser')
            ->with($dto)
            ->willReturn($expectedUser);

        $result = $this->useCase->execute($dto);

        $this->assertSame($expectedUser, $result);
        $this->assertSame('uuid-12345', $result->id);
        $this->assertSame('novousuario@constrsw.pucrs.br', $result->email);
    }

    public function testExecuteFailsWhenEmailIsInvalid(): void
    {
        $dto = new CreateUserDTO(
            username: 'usuario',
            email: 'not-an-email',
            firstName: 'Nome',
            lastName: 'Sobrenome',
            password: 'secretPassword123'
        );

        $this->userPort->expects($this->never())->method('createUser');

        $this->expectException(ValidationException::class);

        try {
            $this->useCase->execute($dto);
        } catch (ValidationException $e) {
            $this->assertSame('VALIDATION_ERROR', $e->getErrorCode());
            $this->assertArrayHasKey('email', $e->getErrors());
            throw $e;
        }
    }

    public function testExecuteFailsWhenRequiredFieldsAreEmpty(): void
    {
        $dto = new CreateUserDTO(
            username: '',
            email: '',
            firstName: '',
            lastName: '',
            password: ''
        );

        $this->userPort->expects($this->never())->method('createUser');

        $this->expectException(ValidationException::class);

        try {
            $this->useCase->execute($dto);
        } catch (ValidationException $e) {
            $this->assertSame('VALIDATION_ERROR', $e->getErrorCode());
            $this->assertArrayHasKey('username', $e->getErrors());
            $this->assertArrayHasKey('email', $e->getErrors());
            $this->assertArrayHasKey('firstName', $e->getErrors());
            $this->assertArrayHasKey('lastName', $e->getErrors());
            $this->assertArrayHasKey('password', $e->getErrors());
            throw $e;
        }
    }

    public function testExecutePropagatesUserAlreadyExistsException(): void
    {
        $dto = new CreateUserDTO(
            username: 'duplicado@constrsw.pucrs.br',
            email: 'duplicado@constrsw.pucrs.br',
            firstName: 'Dup',
            lastName: 'Licado',
            password: 'password123'
        );

        $this->userPort
            ->expects($this->once())
            ->method('createUser')
            ->with($dto)
            ->willThrowException(new UserAlreadyExistsException());

        $this->expectException(UserAlreadyExistsException::class);
        $this->expectExceptionCode(409);

        $this->useCase->execute($dto);
    }
}
