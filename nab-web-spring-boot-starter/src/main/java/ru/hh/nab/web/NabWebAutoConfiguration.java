package ru.hh.nab.web;

import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.PropertySource;
import ru.hh.nab.starter.AppMetadata;
import ru.hh.nab.starter.resource.StatusResource;
import ru.hh.nab.web.jersey.NabResourceConfigCustomizer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for nab web components (servlets, filters, web server customizers and so on).
 */
@AutoConfiguration(before = JerseyAutoConfiguration.class)
@PropertySource("classpath:nab-web.properties")
public class NabWebAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  @Conditional(OnAnyBeanAnnotatedByPathCondition.class)
  public ResourceConfig defaultResourceConfig() {
    return new ResourceConfig();
  }

  @Bean
  @ConditionalOnBean(ResourceConfig.class)
  public ResourceConfigCustomizer nabResourceConfigCustomizer(ApplicationContext applicationContext) {
    return new NabResourceConfigCustomizer(applicationContext);
  }

  @Bean
  public ServletRegistrationBean<ServletContainer> statusServletRegistration(ApplicationContext applicationContext) {
    ResourceConfig statusResourceConfig = new ResourceConfig();
    statusResourceConfig.register(new StatusResource(applicationContext.getBean(AppMetadata.class)));

    ServletRegistrationBean<ServletContainer> registration = new ServletRegistrationBean<>(
        new ServletContainer(statusResourceConfig),
        "/status"
    );
    registration.setName("status");
    registration.setLoadOnStartup(0);
    return registration;
  }

  /**
   * {@link Condition} that checks for the presence of beans annotated with {@link Path @Path}.
   */
  private static class OnAnyBeanAnnotatedByPathCondition extends AllNestedConditions {

    public OnAnyBeanAnnotatedByPathCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnBean(annotation = Path.class)
    private static class BeanAnnotatedByPath {
    }
  }
}
