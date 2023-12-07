package ru.hh.nab.testbase.extensions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

//mostly copy of SpringJUnitWebConfig
@ExtendWith({SpringExtensionWithFailFast.class, NabTestServerExtension.class})
@ContextConfiguration
@WebAppConfiguration
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NabJunitWebConfig {

  /**
   * Alias for {@link ContextConfiguration#classes}.
   */
  @AliasFor(annotation = ContextConfiguration.class, attribute = "classes")
  Class<?>[] value() default {};

  /**
   * Alias for {@link ContextConfiguration#classes}.
   */
  @AliasFor(annotation = ContextConfiguration.class)
  Class<?>[] classes() default {};

  /**
   * Alias for {@link ContextConfiguration#locations}.
   */
  @AliasFor(annotation = ContextConfiguration.class)
  String[] locations() default {};

  /**
   * Alias for {@link ContextConfiguration#initializers}.
   */
  @AliasFor(annotation = ContextConfiguration.class)
  Class<? extends ApplicationContextInitializer<?>>[] initializers() default {};

  /**
   * Alias for {@link ContextConfiguration#inheritLocations}.
   */
  @AliasFor(annotation = ContextConfiguration.class)
  boolean inheritLocations() default true;

  /**
   * Alias for {@link ContextConfiguration#inheritInitializers}.
   */
  @AliasFor(annotation = ContextConfiguration.class)
  boolean inheritInitializers() default true;

  /**
   * Alias for {@link ContextConfiguration#name}.
   */
  @AliasFor(annotation = ContextConfiguration.class)
  String name() default "";

  /**
   * Alias for {@link WebAppConfiguration#value}.
   */
  @AliasFor(annotation = WebAppConfiguration.class, attribute = "value")
  String resourcePath() default "src/main/webapp";
}
