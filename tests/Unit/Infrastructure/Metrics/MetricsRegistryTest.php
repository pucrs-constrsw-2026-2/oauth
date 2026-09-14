<?php

declare(strict_types=1);

namespace App\Tests\Unit\Infrastructure\Metrics;

use App\Infrastructure\Metrics\MetricsRegistry;
use PHPUnit\Framework\TestCase;

final class MetricsRegistryTest extends TestCase
{
    private string $tempFile;
    private MetricsRegistry $registry;

    protected function setUp(): void
    {
        $this->tempFile = sys_get_temp_dir() . '/test_metrics_' . uniqid() . '.json';
        $this->registry = new MetricsRegistry($this->tempFile);
    }

    protected function tearDown(): void
    {
        if (file_exists($this->tempFile)) {
            @unlink($this->tempFile);
        }
    }

    public function testRenderAlwaysContainsPhpRuntimeMetrics(): void
    {
        $output = $this->registry->render();

        $this->assertStringContainsString('# HELP php_info', $output);
        $this->assertStringContainsString('# TYPE php_info gauge', $output);
        $this->assertStringContainsString('php_info{version="', $output);

        $this->assertStringContainsString('# HELP php_memory_bytes', $output);
        $this->assertStringContainsString('# TYPE php_memory_bytes gauge', $output);

        $this->assertStringContainsString('# HELP oauth_service_info', $output);
        $this->assertStringContainsString('service="oauth"', $output);
    }

    public function testIncrementCounterPersistsAndFormatsCorrectly(): void
    {
        $this->registry->incrementCounter('http_requests_total', [
            'method' => 'POST',
            'route' => 'api_login',
            'status' => '200',
        ], 3);

        $output = $this->registry->render();

        $this->assertStringContainsString('# HELP http_requests_total', $output);
        $this->assertStringContainsString('# TYPE http_requests_total counter', $output);
        $this->assertStringContainsString('http_requests_total{method="POST",route="api_login",status="200"} 3', $output);
    }

    public function testSetGaugePersistsAndFormatsCorrectly(): void
    {
        $this->registry->setGauge('active_sessions_gauge', 42, ['realm' => 'constrsw']);

        $output = $this->registry->render();

        $this->assertStringContainsString('# HELP active_sessions_gauge', $output);
        $this->assertStringContainsString('# TYPE active_sessions_gauge gauge', $output);
        $this->assertStringContainsString('active_sessions_gauge{realm="constrsw"} 42', $output);
    }

    public function testRecordDurationIncrementsCountAndSum(): void
    {
        $this->registry->recordDuration('request_time_seconds', 0.15, ['method' => 'GET']);
        $this->registry->recordDuration('request_time_seconds', 0.25, ['method' => 'GET']);

        $output = $this->registry->render();

        $this->assertStringContainsString('request_time_seconds_count{method="GET"} 2', $output);
        $this->assertStringContainsString('request_time_seconds_sum{method="GET"} 0.4', $output);
    }

    public function testResetClearsStoredData(): void
    {
        $this->registry->incrementCounter('sample_counter', [], 10);
        $this->registry->reset();

        $output = $this->registry->render();
        $this->assertStringNotContainsString('sample_counter', $output);
    }
}
