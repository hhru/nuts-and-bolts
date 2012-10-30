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
  // If optional flag is set to true, then current method does not start
  // transaction, but has access to EntityManager via providers.
  //
  // If another method is called and it has transactional annotation with
  // optional() flag set to false, then that method starts its own
  // transaction with its own post commit hooks which are run on return.
  //
  // So, the following code starts 1000 transactions in method2:
  // .....
  // @Transactional
  // void method1() { do something }
  // .....
  // @Transactional(optional = true)
  // void method2() {
  //   for (int 1 = 0; i < 1000; i++) {
  //     method1();
  //   }
  // }
  // ....
  //
  boolean optional() default false;
  boolean rollback() default false;
  Class<? extends Annotation> value() default Default.class;
}
