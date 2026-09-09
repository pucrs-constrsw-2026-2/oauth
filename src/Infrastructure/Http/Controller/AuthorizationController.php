<?php

declare(strict_types=1);

namespace App\Infrastructure\Http\Controller;

use App\Application\DTO\Authorization\AuthorizeRequestDTO;
use App\Domain\Exception\InvalidTokenException;
use App\Domain\Port\Inbound\AuthorizeResourceUseCaseInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;

final class AuthorizationController extends AbstractController
{
    private const BEARER_PREFIX = 'Bearer ';

    public function __construct(
        private readonly AuthorizeResourceUseCaseInterface $authorizeUseCase
    ) {
    }

    #[Route('/authorize', name: 'authz_authorize', methods: ['POST'])]
    public function authorize(Request $request): JsonResponse
    {
        $token = $this->extractBearerToken($request);
        $data = $this->extractRequestData($request);
        $dto = AuthorizeRequestDTO::fromRequest($token, $data);

        $responseDTO = $this->authorizeUseCase->execute($dto);

        return new JsonResponse($responseDTO->toArray(), JsonResponse::HTTP_OK);
    }

    private function extractBearerToken(Request $request): string
    {
        $authHeader = (string) $request->headers->get('Authorization', '');

        if (!$this->hasValidBearerHeader($authHeader)) {
            throw new InvalidTokenException('Token de autorização Bearer ausente ou inválido.');
        }

        $token = trim(substr($authHeader, strlen(self::BEARER_PREFIX)));

        if ($token === '') {
            throw new InvalidTokenException('Token de autorização Bearer ausente ou inválido.');
        }

        return $token;
    }

    private function hasValidBearerHeader(string $authHeader): bool
    {
        return str_starts_with($authHeader, self::BEARER_PREFIX);
    }

    private function extractRequestData(Request $request): array
    {
        if ($request->request->count() > 0) {
            return $request->request->all();
        }

        $rawContent = $request->getContent();
        if (trim($rawContent) === '') {
            return [];
        }

        $decoded = json_decode($rawContent, true);
        return is_array($decoded) ? $decoded : [];
    }
}
