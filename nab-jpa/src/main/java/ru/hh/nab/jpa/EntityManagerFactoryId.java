package ru.hh.nab.jpa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This annotation is necessary in order to be able to identify the EntityManagerFactory in case of using multiple factories (for example it can be
 * useful while sending metrics)
 * All {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean} beans should be marked with this annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EntityManagerFactoryId {
  String value();
}
