package ru.hh.nab.web.spring;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public class NabServletContextInitializerBeans extends ServletContextInitializerBeans {

  private static final String ASYNC_SUPPORTED_ATTRIBUTE = "asyncSupported";
  private static final String LOAD_ON_STARTUP_ATTRIBUTE = "loadOnStartup";
  private static final String NAME_ATTRIBUTE = "name";
  private static final String FILTER_NAME_ATTRIBUTE = "filterName";
  private static final String SERVLET_NAMES_ATTRIBUTE = "servletNames";
  private static final String INIT_PARAMS_ATTRIBUTE = "initParams";
  private static final String VALUE_ATTRIBUTE = "value";
  private static final String URL_PATTERNS_ATTRIBUTE = "urlPatterns";
  private static final String DISPATCHER_TYPES_ATTRIBUTE = "dispatcherTypes";

  private static final String CREATE_REGISTRATION_BEAN_METHOD = ClassUtils
      .getMethod(RegistrationBeanAdapter.class, "createRegistrationBean", String.class, Object.class, int.class)
      .getName();

  public NabServletContextInitializerBeans(ListableBeanFactory beanFactory) {
    super(beanFactory);
  }

  @Override
  protected <T> void addAsRegistrationBean(ListableBeanFactory beanFactory, Class<T> type, RegistrationBeanAdapter<T> adapter) {
    super.addAsRegistrationBean(beanFactory, type, createRegistrationBeanAdapterProxy(adapter));
  }

  @SuppressWarnings("unchecked")
  private <T> RegistrationBeanAdapter<T> createRegistrationBeanAdapterProxy(RegistrationBeanAdapter<T> adapter) {
    return (RegistrationBeanAdapter<T>) Proxy.newProxyInstance(
        RegistrationBeanAdapter.class.getClassLoader(),
        new Class[]{RegistrationBeanAdapter.class},
        (proxy, method, args) -> {
          Object result = method.invoke(adapter, args);
          if (method.getName().equals(CREATE_REGISTRATION_BEAN_METHOD)) {
            Class<?> sourceClass = args[1].getClass();
            AnnotationAttributes annotationAttributes;
            if (Servlet.class.isAssignableFrom(sourceClass) &&
                (annotationAttributes = AnnotatedElementUtils.getMergedAnnotationAttributes(sourceClass, WebServlet.class)) != null) {
              configureServletRegistrationBean((ServletRegistrationBean<Servlet>) result, annotationAttributes);
            } else if (Filter.class.isAssignableFrom(sourceClass) &&
                (annotationAttributes = AnnotatedElementUtils.getMergedAnnotationAttributes(sourceClass, WebFilter.class)) != null) {
              configureFilterRegistrationBean((FilterRegistrationBean<Filter>) result, annotationAttributes);
            }
          }
          return result;
        }
    );
  }

  private void configureServletRegistrationBean(ServletRegistrationBean<Servlet> registrationBean, AnnotationAttributes annotationAttributes) {
    registrationBean.setAsyncSupported(annotationAttributes.getBoolean(ASYNC_SUPPORTED_ATTRIBUTE));
    registrationBean.setInitParameters(extractInitParameters(annotationAttributes));
    registrationBean.setLoadOnStartup(annotationAttributes.getNumber(LOAD_ON_STARTUP_ATTRIBUTE));
    String name = annotationAttributes.getString(NAME_ATTRIBUTE);
    if (StringUtils.hasText(name)) {
      registrationBean.setName(name);
    }
    List<String> urlPatterns = extractUrlPatterns(annotationAttributes);
    if (!urlPatterns.isEmpty()) {
      registrationBean.setUrlMappings(urlPatterns);
    }
  }

  private void configureFilterRegistrationBean(FilterRegistrationBean<Filter> registrationBean, AnnotationAttributes annotationAttributes) {
    registrationBean.setAsyncSupported(annotationAttributes.getBoolean(ASYNC_SUPPORTED_ATTRIBUTE));
    registrationBean.setDispatcherTypes(extractDispatcherTypes(annotationAttributes));
    registrationBean.setInitParameters(extractInitParameters(annotationAttributes));
    String name = annotationAttributes.getString(FILTER_NAME_ATTRIBUTE);
    if (StringUtils.hasText(name)) {
      registrationBean.setName(name);
    }
    registrationBean.setServletNames(Arrays.asList(annotationAttributes.getStringArray(SERVLET_NAMES_ATTRIBUTE)));
    List<String> urlPatterns = extractUrlPatterns(annotationAttributes);
    if (!urlPatterns.isEmpty()) {
      registrationBean.setUrlPatterns(urlPatterns);
    }
  }

  protected final Map<String, String> extractInitParameters(AnnotationAttributes annotationAttributes) {
    WebInitParam[] initParams = annotationAttributes.getAnnotationArray(INIT_PARAMS_ATTRIBUTE, WebInitParam.class);
    return Arrays.stream(initParams).collect(Collectors.toMap(WebInitParam::name, WebInitParam::value));
  }

  protected List<String> extractUrlPatterns(AnnotationAttributes annotationAttributes) {
    String[] value = annotationAttributes.getStringArray(VALUE_ATTRIBUTE);
    String[] urlPatterns = annotationAttributes.getStringArray(URL_PATTERNS_ATTRIBUTE);
    if (urlPatterns.length > 0) {
      Assert.state(value.length == 0, "The urlPatterns and value attributes are mutually exclusive.");
      return Arrays.asList(urlPatterns);
    }
    return Arrays.asList(value);
  }

  private EnumSet<DispatcherType> extractDispatcherTypes(AnnotationAttributes annotationAttributes) {
    DispatcherType[] dispatcherTypes = (DispatcherType[]) annotationAttributes.get(DISPATCHER_TYPES_ATTRIBUTE);
    if (dispatcherTypes.length == 0) {
      return EnumSet.noneOf(DispatcherType.class);
    }
    if (dispatcherTypes.length == 1) {
      return EnumSet.of(dispatcherTypes[0]);
    }
    return EnumSet.of(dispatcherTypes[0], Arrays.copyOfRange(dispatcherTypes, 1, dispatcherTypes.length));
  }
}
