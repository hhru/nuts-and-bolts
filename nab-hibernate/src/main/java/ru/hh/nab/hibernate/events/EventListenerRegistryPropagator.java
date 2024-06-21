package ru.hh.nab.hibernate.events;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;
import java.util.List;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import ru.hh.nab.jpa.EntityManagerFactoryCreationHandler;

public class EventListenerRegistryPropagator implements EntityManagerFactoryCreationHandler {

  private final List<EventListenerRegistryConsumer> eventListenerRegistryConsumers;

  public EventListenerRegistryPropagator(List<EventListenerRegistryConsumer> eventListenerRegistryConsumers) {
    this.eventListenerRegistryConsumers = eventListenerRegistryConsumers;
  }

  @Override
  public void accept(EntityManagerFactory entityManagerFactory, PersistenceUnitInfo persistenceUnitInfo) {
    if (!eventListenerRegistryConsumers.isEmpty()) {
      SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
      ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();
      EventListenerRegistry eventListenerRegistry = serviceRegistry.getService(EventListenerRegistry.class);
      eventListenerRegistryConsumers.forEach(consumer -> consumer.accept(eventListenerRegistry));
    }
  }
}
