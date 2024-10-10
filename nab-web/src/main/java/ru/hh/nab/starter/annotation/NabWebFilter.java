package ru.hh.nab.starter.annotation;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WebFilter
@Inherited
public @interface NabWebFilter {

  /**
   * See {@link WebFilter#filterName()}
   * <p>
   * If not specified the bean name will be used
   */
  @AliasFor(annotation = WebFilter.class)
  String filterName() default "";

  /**
   * See {@link WebFilter#urlPatterns()}
   */
  @AliasFor(annotation = WebFilter.class)
  String[] urlPatterns() default {"/*"};

  /**
   * See {@link WebFilter#initParams()}
   */
  @AliasFor(annotation = WebFilter.class)
  WebInitParam[] initParams() default {};

  /**
   * See {@link WebFilter#asyncSupported()}
   */
  @AliasFor(annotation = WebFilter.class)
  boolean asyncSupported() default true;

  /**
   * See {@link WebFilter#servletNames()}
   */
  @AliasFor(annotation = WebFilter.class)
  String[] servletNames() default {};

  /**
   * See {@link WebFilter#dispatcherTypes()}
   */
  @AliasFor(annotation = WebFilter.class)
  DispatcherType[] dispatcherTypes() default {
      DispatcherType.FORWARD,
      DispatcherType.INCLUDE,
      DispatcherType.REQUEST,
      DispatcherType.ASYNC,
      DispatcherType.ERROR
  };
}
