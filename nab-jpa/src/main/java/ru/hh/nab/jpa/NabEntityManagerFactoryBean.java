package ru.hh.nab.jpa;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;
import java.util.Collection;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

public class NabEntityManagerFactoryBean extends LocalContainerEntityManagerFactoryBean {

  @Nullable
  private Collection<EntityManagerFactoryCreationHandler> entityManagerFactoryCreationHandlers;

  @Override
  protected void postProcessEntityManagerFactory(EntityManagerFactory emf, PersistenceUnitInfo pui) {
    super.postProcessEntityManagerFactory(emf, pui);
    if (entityManagerFactoryCreationHandlers != null) {
      entityManagerFactoryCreationHandlers.forEach(handler -> handler.accept(emf, pui));
    }
  }

  public void setEntityManagerFactoryCreationHandlers(
      @Nullable Collection<EntityManagerFactoryCreationHandler> entityManagerFactoryCreationHandlers
  ) {
    this.entityManagerFactoryCreationHandlers = entityManagerFactoryCreationHandlers;
  }
}
