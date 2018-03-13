package ru.hh.nab.hibernate.transaction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import ru.hh.nab.hibernate.datasource.DataSourceType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExecuteOnDataSource {

  DataSourceType dataSourceType() default DataSourceType.READONLY;

  DataSourceCacheMode cacheMode() default DataSourceCacheMode.NORMAL;
}
