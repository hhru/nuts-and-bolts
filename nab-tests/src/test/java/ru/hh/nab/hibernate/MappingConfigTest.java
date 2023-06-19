package ru.hh.nab.hibernate;

import javax.inject.Inject;
import org.hibernate.SessionFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import static ru.hh.nab.hibernate.HibernateTestConfig.TEST_PACKAGE;
import ru.hh.nab.hibernate.model.TestEntity;
import ru.hh.nab.hibernate.model.test.PackageScanEntity;
import ru.hh.nab.testbase.hibernate.HibernateTestBase;

@ContextConfiguration(classes = {HibernateTestConfig.class})
public class MappingConfigTest extends HibernateTestBase {

  @Inject
  private SessionFactory sessionFactory;

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
  public void hibernateShouldHaveMappedEntities() {
    assertNotNull(sessionFactory.getMetamodel().entity(TestEntity.class));
    assertNotNull(sessionFactory.getMetamodel().entity(PackageScanEntity.class));
  }
}
