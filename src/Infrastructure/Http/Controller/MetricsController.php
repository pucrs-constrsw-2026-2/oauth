<?php

declare(strict_types=1);

namespace App\Infrastructure\Http\Controller;

use App\Infrastructure\Metrics\MetricsRegistry;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

/**
 * Controller responsável por exportar as métricas no formato padrão do Prometheus (OpenMetrics).
 */
class MetricsController extends AbstractController
{
    public function __construct(
        private readonly MetricsRegistry $metricsRegistry
    ) {
    }

    #[Route('/metrics', name: 'api_metrics', methods: ['GET'])]
    public function metrics(): Response
    {
        $content = $this->metricsRegistry->render();

        return new Response($content, Response::HTTP_OK, [
            'Content-Type' => 'text/plain; version=0.0.4; charset=utf-8',
            'Cache-Control' => 'no-cache, no-store, must-revalidate',
        ]);
    }
}
