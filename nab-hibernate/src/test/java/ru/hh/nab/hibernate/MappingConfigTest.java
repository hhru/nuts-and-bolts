package ru.hh.nab.hibernate;

import org.hibernate.SessionFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import static ru.hh.nab.hibernate.HibernateTestBaseConfig.TEST_PACKAGE;
import ru.hh.nab.hibernate.model.TestEntity;
import ru.hh.nab.hibernate.model.test.PackageScanEntity;

import javax.inject.Inject;

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
    assertNotNull(sessionFactory.getTypeHelper().entity(TestEntity.class));
    assertNotNull(sessionFactory.getTypeHelper().entity(PackageScanEntity.class));
  }
}
