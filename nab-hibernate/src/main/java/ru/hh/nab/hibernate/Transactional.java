package ru.hh.nab.hibernate;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR })
public @interface Transactional {

  /** Do not open db transaction, only initialize entity manager, get db connection only per query. <br/>
   * This reduces db connection usage, saving db connection pool. <br/>
   * Mind that this violates 'repeatable read' transaction isolation level, but as soon as 'read committed' is commonly used, that is fine. <br/>
   * If db transaction is already opened this flag is ignored. <br/>
   * If some inner method has @Transactional(readOnly=false) annotation, db transaction will be opened for that particular method. <br/>
   * readOnly and optional flags work the same. If any is true then db transaction will not be opened. <br/>
   * We keep both, because of compatibility and because we do not know which one is better to remove) */
  boolean readOnly() default false;

  /** Use {@link #readOnly} */
  @Deprecated
  boolean optional() default false;

  boolean rollback() default false;

  Class<? extends Annotation> value() default Default.class;
}
