package ru.hh.nab.common.servlet;

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

  public static final int LOWEST_PRIORITY = Integer.MAX_VALUE;
  public static final int HIGHEST_PRIORITY = Integer.MIN_VALUE;
}
