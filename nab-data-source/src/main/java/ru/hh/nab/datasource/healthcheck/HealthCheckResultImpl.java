package ru.hh.nab.datasource.healthcheck;

import ru.hh.nab.jdbc.healthcheck.HealthCheckResult;

public record HealthCheckResultImpl(boolean healthy) implements HealthCheckResult {
}
