package com.tom.wardrobe.monitoring;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrometheusMonitoringConfigTest {

    private static final Path PROJECT_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path PROMETHEUS_CONFIG = PROJECT_ROOT.resolve("monitoring/prometheus.yml");
    private static final Path ALERT_RULES = PROJECT_ROOT.resolve("monitoring/alerts/wardrobe-backend-alerts.yml");

    @Test
    void prometheusConfigLoadsAlertRules() throws IOException {
        Map<String, Object> prometheus = loadYaml(PROMETHEUS_CONFIG);

        Object ruleFiles = prometheus.get("rule_files");
        assertInstanceOf(List.class, ruleFiles);
        assertTrue(((List<?>) ruleFiles).contains("/etc/prometheus/alerts/*.yml"));
    }

    @Test
    void backendAlertRulesContainExpectedAlerts() throws IOException {
        Map<String, Object> alertRules = loadYaml(ALERT_RULES);

        List<?> groups = assertList(alertRules.get("groups"));
        Map<?, ?> firstGroup = assertMap(groups.get(0));
        List<?> rules = assertList(firstGroup.get("rules"));
        List<String> alertNames = rules.stream()
                .map(PrometheusMonitoringConfigTest::assertMap)
                .map(rule -> (String) rule.get("alert"))
                .toList();

        assertTrue(alertNames.contains("WardrobeBackendDown"));
        assertTrue(alertNames.contains("WardrobeBackendHighErrorRate"));
        assertTrue(alertNames.contains("WardrobeBackendHighAverageLatency"));
        assertTrue(alertNames.contains("WardrobeBackendHeapUsageHigh"));
        assertTrue(alertNames.contains("WardrobeBackendCpuUsageHigh"));
        assertTrue(alertNames.contains("WardrobeBackendJdbcPoolNearFull"));
    }

    private static Map<String, Object> loadYaml(Path path) throws IOException {
        assertTrue(Files.exists(path), () -> "Missing file: " + path);
        try (Reader reader = Files.newBufferedReader(path)) {
            Object yaml = new Yaml().load(reader);
            assertInstanceOf(Map.class, yaml);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) yaml;
            return result;
        }
    }

    private static List<?> assertList(Object value) {
        assertInstanceOf(List.class, value);
        return (List<?>) value;
    }

    private static Map<?, ?> assertMap(Object value) {
        assertInstanceOf(Map.class, value);
        return (Map<?, ?>) value;
    }
}
