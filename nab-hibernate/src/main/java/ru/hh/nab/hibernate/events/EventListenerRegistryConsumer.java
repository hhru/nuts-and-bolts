package ru.hh.nab.hibernate.events;

import java.util.function.Consumer;
import org.hibernate.event.service.spi.EventListenerRegistry;

public interface EventListenerRegistryConsumer extends Consumer<EventListenerRegistry> {
}
