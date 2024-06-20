package ru.hh.nab.hibernate.transaction;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.transaction.support.TransactionCallback;

public interface ExecuteOnDataSourceTransactionCallbackFactory {
  TransactionCallback<Object> create(ProceedingJoinPoint pjp, ExecuteOnDataSource executeOnDataSource);
}