package ru.hh.nab.common.servlet;

import org.springframework.core.Ordered;

/**
 * Priorities pool [{@link ServletFilterPriorities#SYSTEM_HIGHEST_PRIORITY}, {@link ServletFilterPriorities#SYSTEM_LOWEST_PRIORITY}] is
 * reserved for {@link SystemFilter system filters} only. If non system filter has priority from reserved pool validation error will occur.
 * <p>
 * Servlet filters are invoked before jax-rs filters (see jax-rs filter priorities in ru.hh.nab.starter.jersey.NabPriorities)
 */
public final class ServletFilterPriorities {

  private ServletFilterPriorities() {
  }

  /**
   * Logging filter priority
   */
  public static final int SYSTEM_LOGGING = -4000;

  /**
   * System header decorator filter priority
   */
  public static final int SYSTEM_HEADER_DECORATOR = -3000;

  /**
   * Observability filter priority
   */
  public static final int SYSTEM_OBSERVABILITY = -2000;

  /**
   * Security authentication filter priority
   */
  public static final int AUTHENTICATION = 1000;

  /**
   * Security authorization filter priority
   */
  public static final int AUTHORIZATION = 2000;

  /**
   * Header decorator filter priority
   */
  public static final int HEADER_DECORATOR = 3000;

  /**
   * Message encoder or decoder filter priority
   */
  public static final int ENTITY_CODER = 4000;

  /**
   * User-level filter priority. This value is also used as a default priority
   */
  public static final int USER = 5000;

  public static final int SYSTEM_LOWEST_PRIORITY = -1;
  public static final int SYSTEM_HIGHEST_PRIORITY = Ordered.HIGHEST_PRECEDENCE;

  public static final int APPLICATION_LOWEST_PRIORITY = Ordered.LOWEST_PRECEDENCE;
  public static final int APPLICATION_HIGHEST_PRIORITY = 0;
}
