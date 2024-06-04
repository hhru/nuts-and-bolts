package ru.hh.nab.hibernate.interceptor;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import ru.hh.nab.hibernate.util.SqlRequestIdCommenter;

public class RequestIdPassingInterceptor implements StatementInspector {
  @Override
  public String inspect(String sql) {
    return SqlRequestIdCommenter.addRequestIdComment(sql);
  }
}
