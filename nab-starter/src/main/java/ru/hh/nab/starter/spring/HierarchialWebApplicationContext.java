package ru.hh.nab.starter.spring;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import javax.servlet.ServletContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import static java.util.stream.Collectors.toMap;

public class HierarchialWebApplicationContext extends AnnotationConfigWebApplicationContext {

  public HierarchialWebApplicationContext(WebApplicationContext parentCtx) {
    setParent(parentCtx);
  }

  @Override
  public void setServletContext(ServletContext servletContext) {
    super.setServletContext(servletContext);
    ConfigurableWebApplicationContext ctx = this;
    while (ctx.getParent() instanceof ConfigurableWebApplicationContext) {
      ctx = (ConfigurableWebApplicationContext) ctx.getParent();
      if (!(ctx instanceof GenericWebApplicationContext) && ctx.getServletConfig() == null) {
        ctx.setServletContext(servletContext);
      }
    }
  }

  @Override
  public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
    return BeanFactoryUtils.beansOfTypeIncludingAncestors(getBeanFactory(), type);
  }

  @Override
  public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
    return Arrays.stream(BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(getBeanFactory(), annotationType))
      .collect(toMap(Function.identity(), this::getBean));
  }
}
