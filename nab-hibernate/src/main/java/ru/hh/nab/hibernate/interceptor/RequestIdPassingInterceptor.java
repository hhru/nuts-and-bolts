package ru.hh.nab.hibernate.interceptor;

import org.hibernate.EmptyInterceptor;
import ru.hh.nab.hibernate.util.SqlRequestIdCommenter;

public class RequestIdPassingInterceptor extends EmptyInterceptor {
  @Override
  public String onPrepareStatement(String sql) {
    return SqlRequestIdCommenter.addRequestIdComment(sql);
  }
}
