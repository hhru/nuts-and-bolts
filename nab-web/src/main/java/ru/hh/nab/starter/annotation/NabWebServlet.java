package ru.hh.nab.starter.annotation;

import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WebServlet
@Inherited
public @interface NabWebServlet {

  /**
   * See {@link WebServlet#name()}
   * <p>
   * If not specified the bean name will be used
   */
  @AliasFor(annotation = WebServlet.class)
  String name() default "";

  /**
   * See {@link WebServlet#urlPatterns()}
   */
  @AliasFor(annotation = WebServlet.class)
  String[] urlPatterns() default {"/*"};

  /**
   * See {@link WebServlet#initParams()}
   */
  @AliasFor(annotation = WebServlet.class)
  WebInitParam[] initParams() default {};

  /**
   * See {@link WebServlet#asyncSupported()}
   */
  @AliasFor(annotation = WebServlet.class)
  boolean asyncSupported() default true;

  /**
   * See {@link WebServlet#loadOnStartup()}
   */
  @AliasFor(annotation = WebServlet.class)
  int loadOnStartup() default 0;
}
