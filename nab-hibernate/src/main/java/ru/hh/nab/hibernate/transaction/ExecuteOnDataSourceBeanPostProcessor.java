package ru.hh.nab.hibernate.transaction;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationConfigurationException;
import ru.hh.nab.datasource.DataSourcePropertiesStorage;
import ru.hh.nab.datasource.DataSourceType;

public class ExecuteOnDataSourceBeanPostProcessor implements BeanPostProcessor {

  private final Map<String, Set<String>> affectedBeans = new HashMap<>();
  private boolean dataSourcesAreReady = false;

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    /*
    Each DataSource created by ru.hh.nab.datasource.DataSourceFactory registers itself as a DataSourceType.
    Given that we rely upon fact that at the moment when all dataSources are injected, all DataSourceTypes
    are known therefore can be validated.
     */
    if (bean instanceof DataSourcesReadyTarget) {
      dataSourcesAreReady = true;
    }

    Class<?> clazz = unwrapProxy(bean);
    for (Method method : clazz.getDeclaredMethods()) {
      ExecuteOnDataSource annotation = method.getAnnotation(ExecuteOnDataSource.class);
      if (annotation != null) {
        affectedBeans.computeIfAbsent(beanName, k -> new HashSet<>());
        affectedBeans.get(beanName).add(annotation.dataSourceType());
      }
    }

    Set<String> unconfiguredDataSources = this.affectedBeans.values().stream()
        .flatMap(Collection::stream)
        .filter(item -> !DataSourcePropertiesStorage.isConfigured(item))
        .collect(Collectors.toSet());
    if (dataSourcesAreReady && !unconfiguredDataSources.isEmpty()) {
      String beansString = affectedBeans.entrySet().stream()
          .filter(item -> item.getValue().stream().anyMatch(unconfiguredDataSources::contains))
          .map(Map.Entry::getKey)
          .collect(Collectors.joining(","));
      String dataSourcesString = String.join(",", unconfiguredDataSources);

      String message = String.format("Non-registered [%s] datasource types used in [%s] for beans [%s]. See [%s] for more information.",
          dataSourcesString, ExecuteOnDataSource.class.getName(), beansString, DataSourceType.class.getName());
      throw new AnnotationConfigurationException(message);
    }

    return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
  }

  private Class<?> unwrapProxy(Object bean) {
    Class<?> clazz = bean.getClass();
    // clazz.getSuperclass() != null is a workaround for several Spring contexts where we have @Bean of type Object
    if (clazz.getSuperclass() != null && (java.lang.reflect.Proxy.isProxyClass(clazz) || org.springframework.cglib.proxy.Proxy.isProxyClass(clazz))) {
      return AopUtils.getTargetClass(clazz);
    }
    return clazz;
  }

}
