package ru.hh.nab.jpa;

import java.util.List;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

public class NabPersistenceUnitPostProcessor implements PersistenceUnitPostProcessor {

  private final List<String> managedClassNames;

  public NabPersistenceUnitPostProcessor(List<String> managedClassNames) {
    this.managedClassNames = managedClassNames;
  }

  @Override
  public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {
    managedClassNames.forEach(pui::addManagedClassName);
  }
}
