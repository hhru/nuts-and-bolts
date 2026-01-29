package ru.hh.nab.datasource.aspect;

import static java.util.Objects.requireNonNull;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import ru.hh.nab.datasource.routing.DataSourceContextUnsafe;

@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionalAspect {

  @Around(
      value = "@annotation(org.springframework.transaction.annotation.Transactional) || @annotation(jakarta.transaction.Transactional)",
      argNames = "pjp"
  )
  public Object executeOnSpecialDataSource(ProceedingJoinPoint pjp) throws Throwable {
    try {
      return DataSourceContextUnsafe.executeOn(
          DataSourceContextUnsafe.getDataSourceName(),
          () -> {
            try {
              return pjp.proceed();
            } catch (Throwable e) {
              throw new TransactionalAspectWrappedException(e);
            }
          }
      );
    } catch (TransactionalAspectWrappedException e) {
      throw e.getCause();
    }
  }

  private static final class TransactionalAspectWrappedException extends RuntimeException {
    public TransactionalAspectWrappedException(Throwable cause) {
      super("Exception from TransactionalAspect", requireNonNull(cause));
    }
  }
}
