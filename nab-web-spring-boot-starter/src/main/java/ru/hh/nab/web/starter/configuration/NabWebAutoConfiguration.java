package ru.hh.nab.web.starter.configuration;

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
import ru.hh.nab.common.servlet.ServletSystemFilterPriorities;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.web.consul.ConsulService;
import ru.hh.nab.web.jersey.filter.CacheFilter;
import ru.hh.nab.web.jersey.resolver.MarshallerContextResolver;
import ru.hh.nab.web.resource.StatusResource;
import ru.hh.nab.web.servlet.filter.CommonHeadersFilter;
import ru.hh.nab.web.servlet.filter.RequestIdLoggingFilter;
import ru.hh.nab.web.servlet.filter.SentryFilter;
import ru.hh.nab.web.starter.configuration.properties.ExtendedServerProperties;
import ru.hh.nab.web.starter.configuration.properties.HttpCacheProperties;
import ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties;
import ru.hh.nab.web.starter.configuration.properties.JaxbProperties;
import ru.hh.nab.web.starter.discovery.ServiceDiscoveryInitializer;
import ru.hh.nab.web.starter.jersey.NabResourceConfigCustomizer;
import ru.hh.nab.web.starter.jetty.MonitoredQueuedThreadPoolFactory;
import ru.hh.nab.web.starter.jetty.NabJettyServerCustomizer;
import ru.hh.nab.web.starter.jetty.NabJettyWebServerFactoryCustomizer;
import ru.hh.nab.web.starter.profile.MainProfile;
import ru.hh.nab.web.starter.servlet.SystemFilterRegistrationBean;

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
    NabLoggingConfiguration.class,

    NabJettyWebServerFactoryCustomizer.class,
    NabJettyServerCustomizer.class,
})
@EnableConfigurationProperties({
    ExtendedServerProperties.class,
    HttpCacheProperties.class,
    JaxbProperties.class,
})
public class NabWebAutoConfiguration {

  @Bean
  public MonitoredQueuedThreadPoolFactory monitoredQueuedThreadPoolFactory(
      InfrastructureProperties infrastructureProperties,
      StatsDSender statsDSender
  ) {
    return new MonitoredQueuedThreadPoolFactory(infrastructureProperties.getServiceName(), statsDSender);
  }

  @Bean
  @MainProfile
  @ConditionalOnBean(ConsulService.class)
  public ServiceDiscoveryInitializer serviceDiscoveryInitializer(ConsulService consulService) {
    return new ServiceDiscoveryInitializer(consulService);
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
  public SystemFilterRegistrationBean<RequestIdLoggingFilter> requestIdLoggingFilter() {
    SystemFilterRegistrationBean<RequestIdLoggingFilter> registration = new SystemFilterRegistrationBean<>(new RequestIdLoggingFilter());
    registration.setOrder(ServletSystemFilterPriorities.SYSTEM_LOGGING);
    return registration;
  }

  @Bean
  public SystemFilterRegistrationBean<SentryFilter> sentryFilter() {
    SystemFilterRegistrationBean<SentryFilter> registration = new SystemFilterRegistrationBean<>(new SentryFilter());
    registration.setOrder(ServletSystemFilterPriorities.SYSTEM_OBSERVABILITY);
    return registration;
  }

  @Bean
  public SystemFilterRegistrationBean<CommonHeadersFilter> commonHeadersFilter() {
    SystemFilterRegistrationBean<CommonHeadersFilter> registration = new SystemFilterRegistrationBean<>(new CommonHeadersFilter());
    registration.setOrder(ServletSystemFilterPriorities.SYSTEM_HEADER_DECORATOR);
    return registration;
  }

  @Bean
  public SystemFilterRegistrationBean<RequestContextFilter> requestContextFilter() {
    SystemFilterRegistrationBean<RequestContextFilter> registration = new SystemFilterRegistrationBean<>(new RequestContextFilter());
    registration.setOrder(ServletSystemFilterPriorities.SYSTEM_HEADER_DECORATOR);
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
