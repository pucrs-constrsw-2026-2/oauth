<?php

declare(strict_types=1);

namespace App\Infrastructure\OpenApi\Attribute;

use Attribute;

#[Attribute(Attribute::TARGET_METHOD | Attribute::TARGET_CLASS)]
final class OpenApiOperation
{
    /**
     * @param string[] $tags
     * @param array<int|string, mixed> $responses
     * @param array<string, mixed>|null $requestBody
     * @param array<int, array<string, mixed>> $parameters
     * @param array<int, array<string, mixed>>|null $security
     */
    public function __construct(
        public ?string $summary = null,
        public ?string $description = null,
        public array $tags = [],
        public ?array $requestBody = null,
        public array $responses = [],
        public array $parameters = [],
        public ?array $security = null,
        public bool $deprecated = false
    ) {
    }
}
