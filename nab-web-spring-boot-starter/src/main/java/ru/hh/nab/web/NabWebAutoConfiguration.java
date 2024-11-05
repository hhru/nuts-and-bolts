package ru.hh.nab.web;

import jakarta.inject.Named;
import jakarta.ws.rs.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.RequestContextFilter;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.common.servlet.ServletFilterPriorities;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.profile.MainProfile;
import ru.hh.nab.starter.consul.ConsulService;
import ru.hh.nab.starter.filters.CommonHeadersFilter;
import ru.hh.nab.starter.filters.NabRequestContextFilter;
import ru.hh.nab.starter.filters.RequestIdLoggingFilter;
import ru.hh.nab.starter.filters.SentryFilter;
import ru.hh.nab.starter.jersey.MarshallerContextResolver;
import ru.hh.nab.starter.resource.StatusResource;
import ru.hh.nab.starter.server.cache.CacheFilter;
import static ru.hh.nab.starter.server.jetty.JettyServerFactory.createJettyThreadPool;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.JETTY;
import ru.hh.nab.starter.server.jetty.MonitoredQueuedThreadPool;
import ru.hh.nab.web.jersey.NabResourceConfigCustomizer;
import ru.hh.nab.web.jetty.NabJettyWebServerFactoryCustomizer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for nab web components (servlets, filters, web server customizers and so on).
 */
@AutoConfiguration(before = JerseyAutoConfiguration.class)
@PropertySource("classpath:nab-web.properties")
@Import({
    NabConsulConfiguration.class,
    NabDeployInfoConfiguration.class,
    NabMetricsConfiguration.class,
    NabTaskSchedulingConfiguration.class,

    NabJettyWebServerFactoryCustomizer.class,
})
@EnableConfigurationProperties({
    HttpCacheProperties.class,
    JaxbProperties.class
})
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
  @ConditionalOnBean(ConsulService.class)
  public ServiceRegistrator serviceRegistrator(ConsulService consulService) {
    return new ServiceRegistrator(consulService);
  }

  @Bean
  @ConditionalOnMissingBean
  @Conditional(OnAnyBeanAnnotatedByPathCondition.class)
  public ResourceConfig defaultResourceConfig() {
    return new ResourceConfig();
  }

  @Bean
  @ConditionalOnBean(ResourceConfig.class)
  public ResourceConfigCustomizer nabResourceConfigCustomizer(
      ApplicationContext applicationContext,
      InfrastructureProperties infrastructureProperties,
      JaxbProperties jaxbProperties,
      StatsDSender statsDSender
  ) {
    Collection<Object> beansWithPathAnnotation = applicationContext.getBeansWithAnnotation(Path.class).values();
    MarshallerContextResolver marshallerContextResolver = new MarshallerContextResolver(
        jaxbProperties.getContextsMaxCollectionSize(),
        infrastructureProperties.getServiceName(),
        statsDSender
    );

    List<Object> components = new ArrayList<>();
    components.add(marshallerContextResolver);
    Optional
        .ofNullable(applicationContext.getBeanProvider(CacheFilter.class).getIfAvailable())
        .ifPresent(components::add);
    components.addAll(beansWithPathAnnotation);
    return new NabResourceConfigCustomizer(components);
  }

  @Bean
  public ServletRegistrationBean<ServletContainer> statusServlet(
      InfrastructureProperties infrastructureProperties,
      BuildProperties buildProperties
  ) {
    StatusResource statusResource = new StatusResource(
        infrastructureProperties.getServiceName(),
        buildProperties.getVersion(),
        infrastructureProperties::getUpTime
    );
    ServletRegistrationBean<ServletContainer> registration = new ServletRegistrationBean<>(
        new ServletContainer(new ResourceConfig().register(statusResource)),
        "/status"
    );
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<RequestIdLoggingFilter> requestIdLoggingFilter() {
    FilterRegistrationBean<RequestIdLoggingFilter> registration = new FilterRegistrationBean<>(new RequestIdLoggingFilter());
    registration.setOrder(ServletFilterPriorities.SYSTEM_LOGGING);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<SentryFilter> sentryFilter() {
    FilterRegistrationBean<SentryFilter> registration = new FilterRegistrationBean<>(new SentryFilter());
    registration.setOrder(ServletFilterPriorities.SYSTEM_OBSERVABILITY);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<CommonHeadersFilter> commonHeadersFilter() {
    FilterRegistrationBean<CommonHeadersFilter> registration = new FilterRegistrationBean<>(new CommonHeadersFilter());
    registration.setOrder(ServletFilterPriorities.SYSTEM_HEADER_DECORATOR);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<RequestContextFilter> requestContextFilter() {
    FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>(new NabRequestContextFilter());
    registration.setOrder(ServletFilterPriorities.SYSTEM_HEADER_DECORATOR);
    return registration;
  }

  @Bean
  @MainProfile
  @ConditionalOnProperty(HttpCacheProperties.HTTP_CACHE_SIZE_PROPERTY)
  public CacheFilter cacheFilter(
      InfrastructureProperties infrastructureProperties,
      HttpCacheProperties httpCacheProperties,
      StatsDSender statsDSender
  ) {
    return new CacheFilter(infrastructureProperties.getServiceName(), DataSize.ofMegabytes(httpCacheProperties.getSizeInMb()), statsDSender);
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
