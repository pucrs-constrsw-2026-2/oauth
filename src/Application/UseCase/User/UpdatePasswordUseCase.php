<?php

declare(strict_types=1);

namespace App\Application\UseCase\User;

use App\Application\DTO\User\UpdatePasswordDTO;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Inbound\UpdatePasswordUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakUserPortInterface;

final class UpdatePasswordUseCase implements UpdatePasswordUseCaseInterface
{
    public function __construct(
        private readonly KeycloakUserPortInterface $userPort
    ) {
    }

    public function execute(string $id, UpdatePasswordDTO $dto): void
    {
        if (trim($id) === '') {
            throw new ValidationException('O ID do usuário é obrigatório.');
        }

        if (trim($dto->password) === '') {
            throw new ValidationException('O campo password é obrigatório e não pode ser vazio.', [
                'password' => 'O campo password é obrigatório.',
            ]);
        }

        $this->userPort->updatePassword($id, $dto);
    }
}
