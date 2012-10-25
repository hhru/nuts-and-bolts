package ru.hh.nab.hibernate;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR })
public @interface Transactional {
  boolean readOnly() default false;
  boolean dummy() default false;
  boolean rollback() default false;
  Class<? extends Annotation> value() default Default.class;
}
