package ru.hh.nab.jpa;

import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.collectingAndThen;
import org.springframework.context.ApplicationContext;

public class EntityManagerFactoryRegistry {

  private final Map<String, EntityManagerFactory> entityManagerFactories = new ConcurrentHashMap<>();

  public EntityManagerFactoryRegistry(ApplicationContext context) {
    Map<String, Optional<String>> entityManagerFactoryBeanNameToId = Arrays
        .stream(context.getBeanNamesForType(EntityManagerFactory.class))
        .collect(Collectors.toMap(
            Function.identity(),
            beanName -> Optional
                .ofNullable(context.findAnnotationOnBean(beanName, EntityManagerFactoryId.class))
                .map(EntityManagerFactoryId::value)
        ));
    List<String> entityManagerFactoriesWithoutId = entityManagerFactoryBeanNameToId
        .entrySet()
        .stream()
        .filter(e -> e.getValue().isEmpty())
        .map(Map.Entry::getKey)
        .toList();
    if (!entityManagerFactoriesWithoutId.isEmpty()) {
      throw new RuntimeException(String.format(
          "The following beans %s must be marked with the %s annotation",
          entityManagerFactoriesWithoutId,
          EntityManagerFactoryId.class.getSimpleName()
      ));
    }
    entityManagerFactoryBeanNameToId.forEach((entityManagerFactoryBeanName, entityManagerFactoryId) -> registerEntityManagerFactory(
        entityManagerFactoryId.get(),
        context.getBean(entityManagerFactoryBeanName, EntityManagerFactory.class)
    ));
  }

  public void registerEntityManagerFactory(String entityManagerFactoryId, EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactories.put(entityManagerFactoryId, entityManagerFactory);
  }

  public Map<String, EntityManagerFactory> getEntityManagerFactories() {
    return getEntityManagerFactories(EntityManagerFactory.class);
  }

  public <T extends EntityManagerFactory> Map<String, T> getEntityManagerFactories(Class<T> cls) {
    return entityManagerFactories
        .entrySet()
        .stream()
        .collect(collectingAndThen(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().unwrap(cls)), Collections::unmodifiableMap));
  }
}
