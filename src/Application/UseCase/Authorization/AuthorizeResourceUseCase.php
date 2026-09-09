<?php

declare(strict_types=1);

namespace App\Application\UseCase\Authorization;

use App\Application\DTO\Authorization\AuthorizeRequestDTO;
use App\Application\DTO\Authorization\AuthorizeResponseDTO;
use App\Domain\Exception\AccessDeniedException;
use App\Domain\Exception\InvalidTokenException;
use App\Domain\Exception\ValidationException;
use App\Domain\Port\Inbound\AuthorizeResourceUseCaseInterface;
use App\Domain\Port\Outbound\KeycloakAuthorizationPortInterface;

final class AuthorizeResourceUseCase implements AuthorizeResourceUseCaseInterface
{
    public function __construct(
        private readonly KeycloakAuthorizationPortInterface $authorizationPort
    ) {
    }

    public function execute(AuthorizeRequestDTO $dto): AuthorizeResponseDTO
    {
        $this->validateInput($dto);

        $response = $this->authorizationPort->validateAccess($dto);

        if (!$response->authorized) {
            throw new AccessDeniedException(
                $response->reason ?? "Acesso negado para o recurso '{$dto->resource}'."
            );
        }

        return $response;
    }

    private function validateInput(AuthorizeRequestDTO $dto): void
    {
        if (trim($dto->accessToken) === '') {
            throw new InvalidTokenException('Token de acesso Bearer ausente ou inválido.');
        }

        if (trim($dto->resource) === '') {
            throw new ValidationException('O campo resource é obrigatório e não pode ser vazio.', [
                'resource' => 'O campo resource é obrigatório.',
            ]);
        }
    }
}
