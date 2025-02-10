package ru.hh.nab.datasource.ext;

/**
 * <p>Extension provides the ability to decorate DataSource with wrapper sending OpenTelemetry spans.
 * This extension used if you create DataSource via {@link ru.hh.nab.datasource.DataSourceFactory}.
 * Pass implementation to {@link ru.hh.nab.datasource.DataSourceFactory} as a parameter instead of <code>null</code> to activate it.<p/>
 *
 * <p>Default implementation placed in nab-telemetry-jdbc module.</p>
 */
@FunctionalInterface
public interface OpenTelemetryJdbcExtension extends JdbcExtension {
}
