package ru.hh.nab.datasource.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.transaction.support.TransactionCallback;
import ru.hh.nab.datasource.annotation.ExecuteOnDataSource;

public interface ExecuteOnDataSourceTransactionCallbackFactory {
  TransactionCallback<Object> create(ProceedingJoinPoint pjp, ExecuteOnDataSource executeOnDataSource);
}
