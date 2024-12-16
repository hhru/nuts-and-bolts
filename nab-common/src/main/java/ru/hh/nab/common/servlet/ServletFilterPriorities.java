package ru.hh.nab.common.servlet;

import org.springframework.core.Ordered;

/**
 * Servlet filters are invoked before jax-rs filters (see jax-rs filter priorities in ru.hh.nab.starter.jersey.NabPriorities)
 */
public final class ServletFilterPriorities {

  private ServletFilterPriorities() {
  }

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

  public static final int LOWEST_PRIORITY = Ordered.LOWEST_PRECEDENCE;
  public static final int HIGHEST_PRIORITY = Ordered.HIGHEST_PRECEDENCE;
}
