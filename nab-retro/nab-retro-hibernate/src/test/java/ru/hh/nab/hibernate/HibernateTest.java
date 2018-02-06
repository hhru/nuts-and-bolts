package ru.hh.nab.hibernate;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.beans.PropertyVetoException;
import java.util.Collections;
import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hsqldb.jdbc.JDBCDriver;
import org.junit.Assert;
import org.junit.Test;

public class HibernateTest {

  @Entity
  @Table(name = "test")
  public static class TestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer id;

    private String name;

    public TestEntity() {
    }

    public TestEntity(String name) {
      this.name = name;
    }

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @Test
  public void test() throws PropertyVetoException {
    ComboPooledDataSource ds = new ComboPooledDataSource();
    ds.setJdbcUrl("jdbc:hsqldb:mem:" + getClass().getName());
    ds.setDriverClass(JDBCDriver.class.getName());
    ds.setUser("sa");
    ds.setPassword("");

    final Properties properties = new Properties();
    properties.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
    properties.setProperty("hibernate.hbm2ddl.auto", "update");
    properties.setProperty("hibernate.format_sql", "true");

    final NaBPersistenceUnitInfo nabPersistenceUnitInfo
            = new NaBPersistenceUnitInfo("default-db", ds, Collections.singletonList(TestEntity.class.getName()), properties);

    final EntityManagerFactory emf = new HibernatePersistenceProvider().createContainerEntityManagerFactory(nabPersistenceUnitInfo, null);

    EntityManager em = emf.createEntityManager();

    em.getTransaction().begin();

    TestEntity e = new TestEntity("42");
    em.persist(e);
    int id = e.getId();
    em.getTransaction().commit();
    em.close();


    em = emf.createEntityManager();
    em.getTransaction().begin();
    Assert.assertEquals("42", em.find(TestEntity.class, id).getName());
    em.getTransaction().commit();
    em.close();
  }
}
