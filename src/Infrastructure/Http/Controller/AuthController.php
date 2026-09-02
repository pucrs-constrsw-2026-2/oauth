<?php

declare(strict_types=1);

namespace App\Infrastructure\Http\Controller;

use App\Application\DTO\Auth\LoginRequestDTO;
use App\Application\DTO\Auth\RefreshTokenRequestDTO;
use App\Domain\Exception\InvalidTokenException;
use App\Domain\Port\Inbound\GetUserInfoUseCaseInterface;
use App\Domain\Port\Inbound\LoginUseCaseInterface;
use App\Domain\Port\Inbound\RefreshTokenUseCaseInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;

final class AuthController extends AbstractController
{
    private const BEARER_PREFIX = 'Bearer ';

    public function __construct(
        private readonly LoginUseCaseInterface $loginUseCase,
        private readonly RefreshTokenUseCaseInterface $refreshTokenUseCase,
        private readonly GetUserInfoUseCaseInterface $getUserInfoUseCase
    ) {
    }

    #[Route('/login', name: 'auth_login', methods: ['POST'])]
    public function login(Request $request): JsonResponse
    {
        $data = $this->extractRequestData($request);
        $dto = LoginRequestDTO::fromArray($data);
        $tokenResponse = $this->loginUseCase->execute($dto);

        return new JsonResponse($tokenResponse->toArray(), JsonResponse::HTTP_OK);
    }

    #[Route('/refresh', name: 'auth_refresh', methods: ['POST'])]
    public function refresh(Request $request): JsonResponse
    {
        $data = $this->extractRequestData($request);
        $dto = RefreshTokenRequestDTO::fromArray($data);
        $tokenResponse = $this->refreshTokenUseCase->execute($dto);

        return new JsonResponse($tokenResponse->toArray(), JsonResponse::HTTP_OK);
    }

    #[Route('/me', name: 'auth_me', methods: ['GET'])]
    public function userInfo(Request $request): JsonResponse
    {
        $token = $this->extractBearerToken($request);
        $userProfile = $this->getUserInfoUseCase->execute($token);

        return new JsonResponse($userProfile->toArray(), JsonResponse::HTTP_OK);
    }

    private function extractRequestData(Request $request): array
    {
        if ($this->hasFormData($request)) {
            return $request->request->all();
        }

        return $this->parseJsonContent($request->getContent());
    }

    private function hasFormData(Request $request): bool
    {
        return $request->request->count() > 0;
    }

    private function parseJsonContent(string $rawContent): array
    {
        if (trim($rawContent) === '') {
            return [];
        }

        $decoded = json_decode($rawContent, true);
        return is_array($decoded) ? $decoded : [];
    }

    private function extractBearerToken(Request $request): string
    {
        $authHeader = (string) $request->headers->get('Authorization', '');

        if (!$this->hasValidBearerHeader($authHeader)) {
            throw new InvalidTokenException('Token de autorização Bearer ausente ou inválido.');
        }

        $token = $this->sanitizeBearerToken($authHeader);

        if ($this->isEmptyToken($token)) {
            throw new InvalidTokenException('Token de autorização Bearer ausente ou inválido.');
        }

        return $token;
    }

    private function hasValidBearerHeader(string $authHeader): bool
    {
        return str_starts_with($authHeader, self::BEARER_PREFIX);
    }

    private function sanitizeBearerToken(string $authHeader): string
    {
        return trim(substr($authHeader, strlen(self::BEARER_PREFIX)));
    }

    private function isEmptyToken(string $token): bool
    {
        return $token === '';
    }
}
