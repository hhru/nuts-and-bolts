package ru.hh.nab.web;

import jakarta.annotation.Nullable;
import jakarta.inject.Named;
import jakarta.servlet.DispatcherType;
import jakarta.ws.rs.Path;
import java.util.EnumSet;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.PropertySource;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.profile.MainProfile;
import ru.hh.nab.starter.AppMetadata;
import ru.hh.nab.starter.consul.ConsulService;
import ru.hh.nab.starter.events.JettyEventListener;
import ru.hh.nab.starter.filters.CommonHeadersFilter;
import ru.hh.nab.starter.filters.RequestIdLoggingFilter;
import ru.hh.nab.starter.resource.StatusResource;
import ru.hh.nab.starter.server.cache.CacheFilter;
import static ru.hh.nab.starter.server.jetty.JettyServerFactory.createJettyThreadPool;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.JETTY;
import ru.hh.nab.starter.server.jetty.MonitoredQueuedThreadPool;
import ru.hh.nab.web.jersey.NabResourceConfigCustomizer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for nab web components (servlets, filters, web server customizers and so on).
 */
@AutoConfiguration(before = JerseyAutoConfiguration.class)
@PropertySource("classpath:nab-web.properties")
@EnableConfigurationProperties(HttpCacheProperties.class)
public class NabWebAutoConfiguration {

  @Bean
  public MonitoredQueuedThreadPool jettyThreadPool(
      FileSettings fileSettings,
      @Named(SERVICE_NAME) String serviceNameValue,
      StatsDSender statsDSender
  ) throws Exception {
    return createJettyThreadPool(fileSettings.getSubSettings(JETTY), serviceNameValue, statsDSender);
  }

  @Bean
  @MainProfile
  public JettyEventListener jettyEventListener(@Nullable ConsulService consulService) {
    return new JettyEventListener(consulService);
  }

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

  @Bean
  public FilterRegistrationBean<RequestIdLoggingFilter> requestIdLoggingFilter() {
    FilterRegistrationBean<RequestIdLoggingFilter> registration = new FilterRegistrationBean<>(new RequestIdLoggingFilter());
    registration.setName(RequestIdLoggingFilter.class.getName());
    registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
    registration.setMatchAfter(true);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<CommonHeadersFilter> commonHeadersFilter() {
    FilterRegistrationBean<CommonHeadersFilter> registration = new FilterRegistrationBean<>(new CommonHeadersFilter());
    registration.setName(CommonHeadersFilter.class.getName());
    registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
    registration.setMatchAfter(true);
    return registration;
  }

  @Bean
  @MainProfile
  @ConditionalOnProperty(prefix = HttpCacheProperties.PREFIX, name = HttpCacheProperties.SIZE_PROPERTY)
  public FilterRegistrationBean<CacheFilter> cacheFilter(HttpCacheProperties httpCacheProperties, String serviceName, StatsDSender statsDSender) {
    CacheFilter cacheFilter = new CacheFilter(serviceName, httpCacheProperties.getSize(), statsDSender);
    FilterRegistrationBean<CacheFilter> registration = new FilterRegistrationBean<>(cacheFilter);
    registration.setName(CacheFilter.class.getName());
    registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
    registration.setMatchAfter(true);
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
