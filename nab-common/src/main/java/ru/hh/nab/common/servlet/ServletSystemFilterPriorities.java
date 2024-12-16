package ru.hh.nab.common.servlet;

import org.springframework.core.Ordered;

/**
 * These priorities are intended for system filters that should be invoked before other plain application filters.
 * Use {@link ServletFilterPriorities} for plain application filters.
 */
public final class ServletSystemFilterPriorities {

  private ServletSystemFilterPriorities() {
  }

  /**
   * Logging filter priority
   */
  public static final int SYSTEM_LOGGING = 1000;

  /**
   * System header decorator filter priority
   */
  public static final int SYSTEM_HEADER_DECORATOR = 2000;

  /**
   * Observability filter priority
   */
  public static final int SYSTEM_OBSERVABILITY = 3000;

  public static final int LOWEST_PRIORITY = Ordered.LOWEST_PRECEDENCE;
  public static final int HIGHEST_PRIORITY = Ordered.HIGHEST_PRECEDENCE;
}
