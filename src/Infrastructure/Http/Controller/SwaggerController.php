<?php

declare(strict_types=1);

namespace App\Infrastructure\Http\Controller;

use App\Infrastructure\OpenApi\OpenApiSpecificationBuilder;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

final class SwaggerController
{
    public function __construct(
        private readonly OpenApiSpecificationBuilder $specBuilder
    ) {
    }

    #[Route('/docs/openapi.json', name: 'api_docs_openapi_json', methods: ['GET'])]
    public function openApiJson(Request $request): JsonResponse
    {
        $serverUrl = $request->getSchemeAndHttpHost();
        $spec = $this->specBuilder->build($serverUrl);

        return new JsonResponse($spec, Response::HTTP_OK, [
            'Access-Control-Allow-Origin' => '*',
            'Access-Control-Allow-Methods' => 'GET, OPTIONS',
            'Content-Type' => 'application/json; charset=utf-8',
        ]);
    }

    #[Route('/docs', name: 'api_docs_ui', methods: ['GET'])]
    public function docsUi(Request $request): Response
    {
        $templatePath = dirname(__DIR__, 4) . '/templates/swagger/index.html';
        if (!file_exists($templatePath)) {
            throw new \RuntimeException("Template do Swagger não encontrado em: {$templatePath}");
        }

        $template = file_get_contents($templatePath);

        $jsonUrl = $request->getBasePath() . '/docs/openapi.json';
        $html = str_replace('{{OPENAPI_JSON_URL}}', $jsonUrl, (string) $template);

        return new Response($html, Response::HTTP_OK, [
            'Content-Type' => 'text/html; charset=utf-8',
        ]);
    }
}
