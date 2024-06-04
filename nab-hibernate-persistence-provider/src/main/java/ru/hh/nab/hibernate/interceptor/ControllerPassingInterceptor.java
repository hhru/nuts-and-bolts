package ru.hh.nab.hibernate.interceptor;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import ru.hh.nab.common.mdc.MDC;

public class ControllerPassingInterceptor implements StatementInspector {
  @Override
  public String inspect(String sql) {
    return MDC.getController().map(s -> "/* " + s.replace('*', '_') + " */" + sql).orElse(sql);
  }
}
