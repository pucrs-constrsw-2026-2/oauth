<?php

declare(strict_types=1);

namespace App\Infrastructure\Metrics;

/**
 * Registry de métricas no padrão OpenMetrics / Prometheus.
 * Permite persistir contadores e medidores entre requisições de forma atômica e performática.
 */
class MetricsRegistry
{
    private string $storageFile;

    public function __construct(?string $storageFile = null)
    {
        $this->storageFile = $storageFile ?? (sys_get_temp_dir() . '/oauth_prometheus_metrics.json');
    }

    /**
     * Incrementa um contador Prometheus.
     *
     * @param array<string, string> $labels
     */
    public function incrementCounter(string $name, array $labels = [], int $by = 1): void
    {
        $data = $this->readData();
        $key = $this->buildMetricKey($name, $labels);

        $current = (int) ($data['counters'][$key]['value'] ?? 0);
        $data['counters'][$key] = [
            'name' => $name,
            'labels' => $labels,
            'value' => $current + $by,
        ];

        $this->writeData($data);
    }

    /**
     * Define o valor instantâneo de um Gauge Prometheus.
     *
     * @param array<string, string> $labels
     */
    public function setGauge(string $name, float|int $value, array $labels = []): void
    {
        $data = $this->readData();
        $key = $this->buildMetricKey($name, $labels);

        $data['gauges'][$key] = [
            'name' => $name,
            'labels' => $labels,
            'value' => $value,
        ];

        $this->writeData($data);
    }

    /**
     * Registra o tempo de duração de uma operação em segundos (Gauge ou sum/count).
     *
     * @param array<string, string> $labels
     */
    public function recordDuration(string $name, float $seconds, array $labels = []): void
    {
        $this->incrementCounter($name . '_count', $labels);
        
        $data = $this->readData();
        $sumKey = $this->buildMetricKey($name . '_sum', $labels);
        $currentSum = (float) ($data['counters'][$sumKey]['value'] ?? 0.0);
        $data['counters'][$sumKey] = [
            'name' => $name . '_sum',
            'labels' => $labels,
            'value' => $currentSum + $seconds,
        ];
        $this->writeData($data);
    }

    /**
     * Reseta todas as métricas armazenadas (ideal para testes).
     */
    public function reset(): void
    {
        if (file_exists($this->storageFile)) {
            @unlink($this->storageFile);
        }
    }

    /**
     * Renderiza todas as métricas no formato canônico do Prometheus (text/plain).
     */
    public function render(): string
    {
        $data = $this->readData();
        $output = [];

        // 1. Métricas do Runtime PHP (sempre frescas e atualizadas dinamicamente)
        $output[] = '# HELP php_info Informações sobre a versão do runtime PHP';
        $output[] = '# TYPE php_info gauge';
        $output[] = sprintf('php_info{version="%s",sapi="%s"} 1', PHP_VERSION, PHP_SAPI);
        $output[] = '';

        $output[] = '# HELP php_memory_bytes Uso atual de memória alocada pelo PHP em bytes';
        $output[] = '# TYPE php_memory_bytes gauge';
        $output[] = sprintf('php_memory_bytes %d', memory_get_usage(false));
        $output[] = '';

        $output[] = '# HELP php_memory_peak_bytes Pico máximo de memória alocada pelo PHP em bytes';
        $output[] = '# TYPE php_memory_peak_bytes gauge';
        $output[] = sprintf('php_memory_peak_bytes %d', memory_get_peak_usage(true));
        $output[] = '';

        // 2. Metadados do Serviço OAuth
        $realm = $_ENV['KEYCLOAK_REALM'] ?? 'constrsw';
        $output[] = '# HELP oauth_service_info Metadados do microserviço de autenticação e autorização';
        $output[] = '# TYPE oauth_service_info gauge';
        $output[] = sprintf('oauth_service_info{service="oauth",framework="symfony",architecture="hexagonal",realm="%s"} 1', $realm);
        $output[] = '';

        // 3. Contadores acumulados
        $counters = $data['counters'] ?? [];
        if (!empty($counters)) {
            $grouped = [];
            foreach ($counters as $metric) {
                $grouped[$metric['name']][] = $metric;
            }

            foreach ($grouped as $metricName => $entries) {
                $output[] = sprintf('# HELP %s Contador acumulado da aplicação', $metricName);
                $output[] = sprintf('# TYPE %s counter', $metricName);
                foreach ($entries as $entry) {
                    $output[] = $this->formatLine($entry['name'], $entry['labels'], $entry['value']);
                }
                $output[] = '';
            }
        }

        // 4. Gauges acumulados
        $gauges = $data['gauges'] ?? [];
        if (!empty($gauges)) {
            $groupedGauges = [];
            foreach ($gauges as $metric) {
                $groupedGauges[$metric['name']][] = $metric;
            }

            foreach ($groupedGauges as $metricName => $entries) {
                $output[] = sprintf('# HELP %s Medição instantânea da aplicação', $metricName);
                $output[] = sprintf('# TYPE %s gauge', $metricName);
                foreach ($entries as $entry) {
                    $output[] = $this->formatLine($entry['name'], $entry['labels'], $entry['value']);
                }
                $output[] = '';
            }
        }

        return implode("\n", $output) . "\n";
    }

    /**
     * @param array<string, string> $labels
     */
    private function buildMetricKey(string $name, array $labels): string
    {
        ksort($labels);
        return $name . ':' . serialize($labels);
    }

    /**
     * @param array<string, string> $labels
     */
    private function formatLine(string $name, array $labels, float|int $value): string
    {
        if (empty($labels)) {
            return sprintf('%s %s', $name, (string) $value);
        }

        $labelPairs = [];
        foreach ($labels as $k => $v) {
            $escaped = str_replace(['\\', '"', "\n"], ['\\\\', '\"', '\n'], (string) $v);
            $labelPairs[] = sprintf('%s="%s"', $k, $escaped);
        }

        return sprintf('%s{%s} %s', $name, implode(',', $labelPairs), (string) $value);
    }

    /**
     * @return array{counters: array<string, mixed>, gauges: array<string, mixed>}
     */
    private function readData(): array
    {
        if (!file_exists($this->storageFile)) {
            return ['counters' => [], 'gauges' => []];
        }

        $handle = @fopen($this->storageFile, 'r');
        if (!$handle) {
            return ['counters' => [], 'gauges' => []];
        }

        @flock($handle, LOCK_SH);
        $content = @stream_get_contents($handle);
        @flock($handle, LOCK_UN);
        @fclose($handle);

        if (!$content) {
            return ['counters' => [], 'gauges' => []];
        }

        $decoded = json_decode($content, true);
        if (!is_array($decoded)) {
            return ['counters' => [], 'gauges' => []];
        }

        return [
            'counters' => $decoded['counters'] ?? [],
            'gauges' => $decoded['gauges'] ?? [],
        ];
    }

    /**
     * @param array{counters: array<string, mixed>, gauges: array<string, mixed>} $data
     */
    private function writeData(array $data): void
    {
        $dir = dirname($this->storageFile);
        if (!is_dir($dir)) {
            @mkdir($dir, 0777, true);
        }

        $handle = @fopen($this->storageFile, 'c+');
        if (!$handle) {
            return;
        }

        if (@flock($handle, LOCK_EX)) {
            @ftruncate($handle, 0);
            @rewind($handle);
            @fwrite($handle, (string) json_encode($data, JSON_UNESCAPED_UNICODE));
            @fflush($handle);
            @flock($handle, LOCK_UN);
        }

        @fclose($handle);
    }
}
