package ru.hh.nab.common.servlet;

/**
 * Priority constants for servlet filters.
 * It's a copy of {@link ru.hh.nab.starter.jersey.NabPriorities}.
 */
public class ServletFilterPriorities {

  /**
   * Observability filter/interceptor priority.
   */
  public static final int OBSERVABILITY = 500;

  /**
   * Cache filter/interceptor priority.
   */
  public static final int CACHE = 750;

  /**
   * Security authentication filter/interceptor priority.
   */
  public static final int AUTHENTICATION = 1000;

  /**
   * Security authorization filter/interceptor priority.
   */
  public static final int AUTHORIZATION = 2000;

  /**
   * Header decorator filter/interceptor priority.
   */
  public static final int HEADER_DECORATOR = 3000;

  /**
   * Message encoder or decoder filter/interceptor priority.
   */
  public static final int ENTITY_CODER = 4000;

  /**
   * User-level filter/interceptor priority.
   */
  public static final int USER = 5000;

  private ServletFilterPriorities() {
  }
}
