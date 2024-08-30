package ru.hh.nab.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;

/**
 * This annotation is a copy of {@link org.springframework.boot.autoconfigure.SpringBootApplication @SpringBootApplication} but without triggering
 * component scanning.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration
@EnableAutoConfiguration
public @interface NabApplication {

  /**
   * See {@link SpringBootApplication#exclude()}
   */
  @AliasFor(annotation = EnableAutoConfiguration.class)
  Class<?>[] exclude() default {};

  /**
   * See {@link SpringBootApplication#excludeName()}
   */
  @AliasFor(annotation = EnableAutoConfiguration.class)
  String[] excludeName() default {};

  /**
   * See {@link SpringBootApplication#proxyBeanMethods()}
   */
  @AliasFor(annotation = Configuration.class)
  boolean proxyBeanMethods() default true;
}
