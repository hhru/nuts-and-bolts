package ru.hh.nab.testbase.extensions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HHJetty {
  int port() default 9000;
  Class<? extends OverrideNabApplication> overrideApplication() default OverrideNabApplication.class;
}
