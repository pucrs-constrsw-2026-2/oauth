<?php

declare(strict_types=1);

namespace App\Tests\Unit\Application\UseCase\User;

use App\Application\DTO\User\UserDTO;
use App\Application\UseCase\User\ListUsersUseCase;
use App\Domain\Port\Outbound\KeycloakUserPortInterface;
use PHPUnit\Framework\TestCase;

final class ListUsersUseCaseTest extends TestCase
{
    private KeycloakUserPortInterface $userPort;
    private ListUsersUseCase $useCase;

    protected function setUp(): void
    {
        $this->userPort = $this->createMock(KeycloakUserPortInterface::class);
        $this->useCase = new ListUsersUseCase($this->userPort);
    }

    public function testExecuteReturnsListOfActiveUsers(): void
    {
        $users = [
            new UserDTO(
                id: '1',
                username: 'user1@example.com',
                email: 'user1@example.com',
                firstName: 'User',
                lastName: 'One',
                enabled: true,
                roles: ['professor']
            ),
            new UserDTO(
                id: '2',
                username: 'user2@example.com',
                email: 'user2@example.com',
                firstName: 'User',
                lastName: 'Two',
                enabled: true,
                roles: ['student']
            ),
        ];

        $this->userPort
            ->expects($this->once())
            ->method('listActiveUsers')
            ->willReturn($users);

        $result = $this->useCase->execute();

        $this->assertCount(2, $result);
        $this->assertSame($users, $result);
        $this->assertTrue($result[0]->enabled);
        $this->assertTrue($result[1]->enabled);
    }

    public function testExecuteReturnsEmptyListWhenNoUsers(): void
    {
        $this->userPort
            ->expects($this->once())
            ->method('listActiveUsers')
            ->willReturn([]);

        $result = $this->useCase->execute();

        $this->assertIsArray($result);
        $this->assertEmpty($result);
    }
}
