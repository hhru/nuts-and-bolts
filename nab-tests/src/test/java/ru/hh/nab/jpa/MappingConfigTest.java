package ru.hh.nab.jpa;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import static ru.hh.nab.jpa.JpaTestConfig.TEST_PACKAGE;
import ru.hh.nab.jpa.model.PackageNotScanEntity;
import ru.hh.nab.jpa.model.TestEntity;
import ru.hh.nab.jpa.model.test.PackageScanEntity;
import ru.hh.nab.testbase.jpa.JpaTestBase;

@ContextConfiguration(classes = {JpaTestConfig.class})
public class MappingConfigTest extends JpaTestBase {

  @Inject
  private MappingConfig mappingConfig;

  @Test
  public void shouldReturnEntities() {
    Class<?>[] classes = mappingConfig.getAnnotatedClasses();

    assertEquals(1, classes.length);
    assertEquals(TestEntity.class, classes[0]);
  }

  @Test
  public void shouldReturnPackages() {
    String[] packages = mappingConfig.getPackagesToScan();

    assertEquals(1, packages.length);
    assertEquals(TEST_PACKAGE, packages[0]);
  }

  @Test
  public void persistenceProviderShouldHaveMappedEntities() {
    assertTrue(metamodelContainsEntity(entityManager, TestEntity.class));
    assertTrue(metamodelContainsEntity(entityManager, PackageScanEntity.class));
    assertFalse(metamodelContainsEntity(entityManager, PackageNotScanEntity.class));
  }

  private boolean metamodelContainsEntity(EntityManager entityManager, Class<?> cls) {
    return entityManager
        .getEntityManagerFactory()
        .getMetamodel()
        .getEntities()
        .stream()
        .anyMatch(e -> e.getJavaType().equals(cls));
  }
}
