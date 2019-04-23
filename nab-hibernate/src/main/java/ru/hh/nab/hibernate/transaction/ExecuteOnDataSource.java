package ru.hh.nab.hibernate.transaction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import ru.hh.nab.datasource.DataSourceType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExecuteOnDataSource {

  boolean writableTx() default false;

  String dataSourceType() default DataSourceType.READONLY;

  boolean overrideByRequestScope() default false;

  DataSourceCacheMode cacheMode() default DataSourceCacheMode.NORMAL;
}
