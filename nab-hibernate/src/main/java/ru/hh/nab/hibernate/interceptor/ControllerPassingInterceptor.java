package ru.hh.nab.hibernate.interceptor;

import org.hibernate.EmptyInterceptor;
import ru.hh.nab.common.mdc.MDC;

public class ControllerPassingInterceptor extends EmptyInterceptor {
  @Override
  public String onPrepareStatement(String sql) {
    return MDC.getController().map(s -> "/* " + s.replace('*', '_') + " */" + sql).orElse(sql);
  }
}
