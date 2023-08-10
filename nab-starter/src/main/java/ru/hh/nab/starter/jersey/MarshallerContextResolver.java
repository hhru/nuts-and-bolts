package ru.hh.nab.starter.jersey;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.util.Collections;
import java.util.Set;
import org.springframework.core.serializer.support.SerializationFailedException;
import ru.hh.nab.common.cache.PartiallyOverflowingCache;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import ru.hh.nab.metrics.TaggedSender;

@Provider
@ApplicationScoped
public class MarshallerContextResolver implements ContextResolver<Marshaller> {
  private final int maxCollectionSize;
  private static final int defaultMaxCollectionSize = 256;
  private final PartiallyOverflowingCache<Class<?>, JAXBContext> jaxbContexts;

  @Inject
  public MarshallerContextResolver(FileSettings fileSettings, StatsDSender statsDSender) {
    String serviceName = fileSettings.getNotEmptyOrThrow(SERVICE_NAME);
    maxCollectionSize = fileSettings.getInteger("jaxbContexts.max.collection.size", defaultMaxCollectionSize);
    jaxbContexts = new PartiallyOverflowingCache<>(maxCollectionSize);

    String cacheSizeMetricName = "JAXBContextCacheSize";
    String cacheMaxSizeMetricName = "JAXBContextCacheMaxSize";
    var sender = new TaggedSender(statsDSender, Set.of(new Tag(Tag.APP_TAG_NAME, serviceName)));
    statsDSender.sendPeriodically(() -> {
          sender.sendGauge(cacheSizeMetricName, jaxbContexts.getStorageSize());
          sender.sendGauge(cacheMaxSizeMetricName, maxCollectionSize);
        }
    );
  }

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
      marshaller.setProperty("org.glassfish.jaxb.characterEscapeHandler", XmlEscapeHandler.INSTANCE);
      return marshaller;
    } catch (JAXBException e) {
      throw new SerializationFailedException("Failed to create Marshaller", e);
    }
  }
}
