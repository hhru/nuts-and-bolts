package ru.hh.nab.starter.jersey;

import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.util.ConcurrentReferenceHashMap;

import javax.ws.rs.ext.ContextResolver;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.util.Collections;
import java.util.Map;

public class MarshallerContextResolver implements ContextResolver<Marshaller> {
  private static final Map<Class<?>, JAXBContext> jaxbContexts =
    new ConcurrentReferenceHashMap<>(16, 0.75f, 16, ConcurrentReferenceHashMap.ReferenceType.WEAK);

  @Override
  public Marshaller getContext(Class<?> type) {
    JAXBContext jaxbContext = jaxbContexts.computeIfAbsent(type, clazz -> {
      try {
        return JAXBContext.newInstance(new Class[] {clazz}, Collections.emptyMap());
      } catch (JAXBException e) {
        throw new SerializationFailedException("Failed to create JAXBContext", e);
      }
    });

    try {
      Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.setProperty("com.sun.xml.bind.characterEscapeHandler", XmlEscapeHandler.INSTANCE);
      return marshaller;
    } catch (JAXBException e) {
      throw new SerializationFailedException("Failed to create Marshaller", e);
    }
  }
}
