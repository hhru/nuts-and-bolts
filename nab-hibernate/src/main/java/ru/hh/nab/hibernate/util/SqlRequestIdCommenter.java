package ru.hh.nab.hibernate.util;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.mdc.MDC;

public final class SqlRequestIdCommenter {
  private static final Predicate<String> SQL_REQUEST_ID_IS_VALID = Pattern.compile("^[a-z0-9_-]+$", CASE_INSENSITIVE).asPredicate();
  private static final Logger LOGGER = LoggerFactory.getLogger(SqlRequestIdCommenter.class);

  private SqlRequestIdCommenter() {}

  public static String addRequestIdComment(final String sql) {
    return MDC.getRequestId().map(requestId -> {
      // check for sql injections and reasonable length
      if (requestId.length() > 100 || !SQL_REQUEST_ID_IS_VALID.test(requestId)) {
        LOGGER.warn("Errant request id, not including it to SQL query: {}", requestId);
        return sql;
      }
      return "/* " + requestId + " */ " + sql;
    }).orElse(sql);
  }
}
