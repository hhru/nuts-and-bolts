package ru.hh.nab.hibernate.transaction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExecuteOnDataSource {

  String dataSourceType() default "readonly";

  boolean overrideByRequestScopeDs() default false;

  DataSourceCacheMode cacheMode() default DataSourceCacheMode.NORMAL;
}
