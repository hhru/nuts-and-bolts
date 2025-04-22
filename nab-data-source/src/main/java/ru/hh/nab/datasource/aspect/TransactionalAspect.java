package ru.hh.nab.datasource.aspect;

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
              throw new ExecuteOnDataSourceWrappedException(e);
            }
          }
      );
    } catch (ExecuteOnDataSourceWrappedException e) {
      throw e.getCause();
    }
  }
}
