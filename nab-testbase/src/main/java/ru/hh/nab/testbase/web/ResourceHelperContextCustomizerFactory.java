package ru.hh.nab.testbase.web;

import java.util.List;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;

public class ResourceHelperContextCustomizerFactory implements ContextCustomizerFactory {

  @Override
  public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
    SpringBootTest springBootTest = TestContextAnnotationUtils.findMergedAnnotation(testClass, SpringBootTest.class);
    return (springBootTest != null && springBootTest.webEnvironment().isEmbedded()) ? new ResourceHelperContextCustomizer() : null;
  }

  private static class ResourceHelperContextCustomizer implements ContextCustomizer {

    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
      if (context.getBeanFactory() instanceof BeanDefinitionRegistry registry) {
        RootBeanDefinition definition = new RootBeanDefinition(
            ResourceHelper.class,
            BeanDefinition.SCOPE_SINGLETON,
            // When ResourceHelper is created environment doesn't contain property local.server.port here (it will be added to environment later).
            // So we can't pass server port to ResourceHelper. That's we pass Supplier here.
            () -> new ResourceHelper(() -> context.getEnvironment().getRequiredProperty("local.server.port", Integer.class))
        );
        registry.registerBeanDefinition(ResourceHelper.class.getName(), definition);
      }
    }

    // Context customizers are used as part of merged spring context configuration. See MergedContextConfiguration#contextCustomizers.
    // Without equals and hashcode methods configuration caching works incorrectly and test server ups on each test class.
    // So don't remove equals and hashcode methods to avoid test performance issues
    @Override
    public boolean equals(Object obj) {
      return (obj != null) && (obj.getClass() == getClass());
    }

    @Override
    public int hashCode() {
      return getClass().hashCode();
    }
  }
}
