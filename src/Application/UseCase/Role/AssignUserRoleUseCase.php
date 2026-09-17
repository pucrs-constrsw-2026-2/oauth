<?php

declare(strict_types=1);

namespace App\Application\UseCase\Role;

use App\Application\DTO\Role\AssignRoleDTO;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Inbound\AssignUserRoleUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakRolePortInterface;

final class AssignUserRoleUseCase implements AssignUserRoleUseCaseInterface
{
    public function __construct(
        private readonly KeycloakRolePortInterface $rolePort
    ) {
    }

    /**
     * @return array<string, mixed>
     */
    public function execute(string $userId, AssignRoleDTO $dto): array
    {
        if (trim($userId) === '') {
            throw new ValidationException('O ID do usuário é obrigatório.', ['userId' => 'O ID do usuário é obrigatório.']);
        }

        $roleIdentifier = $dto->getRoleIdentifier();
        if ($roleIdentifier === null || trim($roleIdentifier) === '') {
            throw new ValidationException('O identificador do papel (roleId ou name) é obrigatório.', [
                'role' => 'Informe roleId ou name.',
            ]);
        }

        return $this->rolePort->assignRoleToUser($userId, $roleIdentifier);
    }
}
