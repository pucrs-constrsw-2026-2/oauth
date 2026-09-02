<?php

declare(strict_types=1);

namespace App\Infrastructure\Http\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Annotation\Route;

class HealthController extends AbstractController
{
    #[Route('/api/health', name: 'api_health', methods: ['GET'])]
    #[Route('/', name: 'api_root', methods: ['GET'])]
    public function health(): JsonResponse
    {
        return new JsonResponse([
            'status' => 'healthy',
            'service' => 'oauth',
            'architecture' => 'hexagonal',
            'framework' => 'symfony',
            'time' => date(DATE_ATOM),
            'keycloak_realm' => $_ENV['KEYCLOAK_REALM'] ?? 'constrsw',
        ]);
    }
}
