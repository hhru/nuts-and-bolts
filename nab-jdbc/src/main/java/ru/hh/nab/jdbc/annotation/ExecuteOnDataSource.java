package ru.hh.nab.jdbc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import ru.hh.nab.jdbc.DataSourceType;

/**
 * Warning: unlike {@link org.springframework.transaction.annotation.Transactional}
 * or {@link jakarta.transaction.Transactional},
 * transactions created by this annotation will rollback on any exception.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExecuteOnDataSource {

  boolean writableTx() default false;

  /**
   * see {@link DataSourceType} for common datasource types
   */
  String dataSourceType();

  boolean overrideByRequestScope() default false;

  /**
   * JPA specific attribute. So this attribute is processed only if you use nab-jpa module. Otherwise, it's useless
   */
  DataSourceCacheMode cacheMode() default DataSourceCacheMode.NORMAL;

  String txManager() default "";
}
