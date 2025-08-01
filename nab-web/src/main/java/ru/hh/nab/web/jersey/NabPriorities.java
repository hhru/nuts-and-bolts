package ru.hh.nab.web.jersey;

import jakarta.ws.rs.Priorities;
import ru.hh.nab.common.servlet.ServletFilterPriorities;

/**
 * Nab-specific priority constants
 * Extend {@link jakarta.ws.rs.Priorities} with custom priority levels
 * <p>
 * Jax-rs filters are invoked after servlet filters (see servlet filter priorities in {@link ServletFilterPriorities})
 */
public class NabPriorities {

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
