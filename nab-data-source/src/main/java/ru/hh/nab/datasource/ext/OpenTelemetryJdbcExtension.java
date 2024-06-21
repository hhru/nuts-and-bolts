package ru.hh.nab.datasource.ext;

/**
 * <p>Extension provides the ability to decorate DataSource with wrapper sending OpenTelemetry spans.
 * This extension used if you create DataSource via {@link ru.hh.nab.datasource.DataSourceFactory}.
 * To activate the extension simply implement it as a Spring bean.<p/>
 *
 * <p>Default implementation placed in nab-telemetry-jdbc module.</p>
 */
@FunctionalInterface
public interface OpenTelemetryJdbcExtension extends JdbcExtension {
}
