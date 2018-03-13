package ru.hh.nab.hibernate.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.core.util.MDC;

public final class SqlRequestIdCommenter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SqlRequestIdCommenter.class);

  private SqlRequestIdCommenter() {}

  public static String addRequestIdComment(final String sql) {
    return MDC.getRequestId().map(requestId -> {
      // check for sql injections and reasonable length
      if (requestId.length() > 100 || !StringUtils.isAlphanumeric(requestId)) {
        LOGGER.warn("Errant request id, not including it to SQL query: {}", requestId);
        return sql;
      }
      return "/* " + requestId + " */ " + sql;
    }).orElse(sql);
  }
}
