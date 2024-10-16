package ru.hh.nab.starter.jersey;


import jakarta.ws.rs.Priorities;

/**
 * Nab-specific priority constants
 * Extend {@link jakarta.ws.rs.Priorities} with custom priority levels
 */
public class NabPriorities  {

  /**
   * Observability filter/interceptor priority.
   */
  public static final int OBSERVABILITY = 500;

  /**
   * Cache filter/interceptor priority.
   */
  public static final int CACHE = 750;

  /**
   * Filter/interceptor priority for post user stage.
   */
  public static final int LOW_PRIORITY = Priorities.USER + 1;

}
