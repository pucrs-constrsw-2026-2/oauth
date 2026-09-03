<?php

declare(strict_types=1);

namespace App\Infrastructure\Http\Listener;

use App\Domain\Exception\DomainException;
use App\Domain\Exception\ValidationException;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpKernel\Event\ExceptionEvent;
use Symfony\Component\HttpKernel\Exception\HttpExceptionInterface;
use Throwable;

class JsonExceptionListener
{
    public function onKernelException(ExceptionEvent $event): void
    {
        $throwable = $event->getThrowable();
        $response = $this->createJsonResponse($throwable);
        $event->setResponse($response);
    }

    public function createJsonResponse(Throwable $exception): JsonResponse
    {
        $statusCode = 500;
        $code = 'INTERNAL_SERVER_ERROR';
        $message = 'Ocorreu um erro interno no servidor.';
        $details = [];

        if ($exception instanceof DomainException) {
            $statusCode = $exception->getHttpStatusCode();
            $code = $exception->getErrorCode();
            $message = $exception->getMessage();

            if ($exception instanceof ValidationException) {
                $details = $exception->getErrors();
            }
        } elseif ($exception instanceof HttpExceptionInterface) {
            $statusCode = $exception->getStatusCode();
            $code = 'HTTP_' . $statusCode;
            $message = $exception->getMessage();
        } else {
            $debug = filter_var($_ENV['APP_DEBUG'] ?? getenv('APP_DEBUG') ?? false, FILTER_VALIDATE_BOOLEAN);
            if ($debug) {
                $message = $exception->getMessage();
                $details = [
                    'file' => $exception->getFile(),
                    'line' => $exception->getLine(),
                ];
            }
        }

        return new JsonResponse([
            'error' => [
                'code' => $code,
                'message' => $message,
                'details' => $details,
            ],
        ], $statusCode);
    }

    public static function handle(Throwable $exception): void
    {
        $listener = new self();
        $response = $listener->createJsonResponse($exception);
        $response->send();
        exit(0);
    }
}
