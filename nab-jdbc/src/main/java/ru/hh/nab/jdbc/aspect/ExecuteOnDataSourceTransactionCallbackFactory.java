package ru.hh.nab.jdbc.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.transaction.support.TransactionCallback;
import ru.hh.nab.jdbc.annotation.ExecuteOnDataSource;

public interface ExecuteOnDataSourceTransactionCallbackFactory {
  TransactionCallback<Object> create(ProceedingJoinPoint pjp, ExecuteOnDataSource executeOnDataSource);
}
