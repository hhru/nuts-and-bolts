package ru.hh.nab.jpa;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;

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

  @Override
  public void setManagedTypes(PersistenceManagedTypes managedTypes) {
    List<String> mergedManagedClassNames = new ArrayList<>(managedTypes.getManagedClassNames());

    List<String> managedPackages = managedTypes.getManagedPackages();
    if (!managedPackages.isEmpty()) {
      PersistenceManagedTypes scannedManagedTypes = scanPackages(managedPackages);
      mergedManagedClassNames.addAll(scannedManagedTypes.getManagedClassNames());
    }
    super.setManagedTypes(PersistenceManagedTypes.of(mergedManagedClassNames, managedPackages));
  }

  public void setEntityManagerFactoryCreationHandlers(
      @Nullable Collection<EntityManagerFactoryCreationHandler> entityManagerFactoryCreationHandlers
  ) {
    this.entityManagerFactoryCreationHandlers = entityManagerFactoryCreationHandlers;
  }

  private PersistenceManagedTypes scanPackages(List<String> packagesToScan) {
    PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    return new PersistenceManagedTypesScanner(resourcePatternResolver).scan(packagesToScan.toArray(new String[]{}));
  }
}
