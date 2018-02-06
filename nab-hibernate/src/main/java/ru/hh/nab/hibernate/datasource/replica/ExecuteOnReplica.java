package ru.hh.nab.hibernate.datasource.replica;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExecuteOnReplica {

  ReplicaCacheMode cacheMode() default ReplicaCacheMode.NORMAL;
}
