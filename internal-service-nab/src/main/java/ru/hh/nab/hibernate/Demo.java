package ru.hh.nab.hibernate;

import com.google.inject.*;
import org.hibernate.Session;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;

import javax.persistence.*;

public class Demo {

  @Entity
  @Table(name = "test")
  public static class TestEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Integer id;
  }

  @Inject Provider<Session> session;

  @Transactional
  public void createAndSaveEntity() {
    TestEntity test = new TestEntity();
    this.session.get().save(test);
  }

  public static void main(String args[]) {
    Injector injector = Guice.createInjector(new HibernateModule(), new AbstractModule() {
      @Override
      protected void configure() {
        bind(Demo.class).in(Scopes.SINGLETON);
      }

      @Provides @Singleton
      protected Configuration configuration() {
        AnnotationConfiguration config = new AnnotationConfiguration();
        config.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        config.setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:test");
        config.setProperty("hibernate.connection.username", "sa");
        config.setProperty("hibernate.connection.password", "");
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        config.setProperty("hibernate.hbm2ddl.auto", "update");
        config.setProperty("hibernate.show_sql", "true");
        config.setProperty("hibernate.format_sql", "true");
        config.setProperty("hibernate.transaction.factory_class", "org.hibernate.transaction.JDBCTransactionFactory");
        config.setProperty("hibernate.current_session_context_class", "thread");
        config.addAnnotatedClass(TestEntity.class);
        return config;
      }
    });
    Demo demo = injector.getInstance(Demo.class);
    demo.createAndSaveEntity();
  }
}

