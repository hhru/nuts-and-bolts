package ru.hh.nab.jpa;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface EntityManagerFactoryCreationHandler extends BiConsumer<EntityManagerFactory, PersistenceUnitInfo> {
}
