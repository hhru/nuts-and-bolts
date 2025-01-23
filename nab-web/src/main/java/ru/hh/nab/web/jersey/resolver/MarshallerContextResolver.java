package ru.hh.nab.web.jersey.resolver;

import jakarta.inject.Inject;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import org.springframework.core.serializer.support.SerializationFailedException;
import ru.hh.nab.common.properties.PropertiesUtils;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import ru.hh.nab.metrics.TaggedSender;

public class MarshallerContextResolver implements ContextResolver<Marshaller> {

  public static final String JAXB_CONTEXTS_MAX_COLLECTION_SIZE_PROPERTY = "jaxbContexts.max.collection.size";
  public static final int DEFAULT_JAXB_CONTEXTS_MAX_COLLECTION_SIZE = 256;

  private final PartiallyOverflowingCache<Class<?>, JAXBContext> jaxbContexts;

  @Inject
  public MarshallerContextResolver(Properties properties, String serviceName, StatsDSender statsDSender) {
    int contextsMaxCollectionSize = PropertiesUtils.getInteger(
        properties,
        JAXB_CONTEXTS_MAX_COLLECTION_SIZE_PROPERTY,
        DEFAULT_JAXB_CONTEXTS_MAX_COLLECTION_SIZE
    );
    jaxbContexts = new PartiallyOverflowingCache<>(contextsMaxCollectionSize);

    String cacheSizeMetricName = "JAXBContextCacheSize";
    String cacheMaxSizeMetricName = "JAXBContextCacheMaxSize";
    var sender = new TaggedSender(statsDSender, Set.of(new Tag(Tag.APP_TAG_NAME, serviceName)));
    statsDSender.sendPeriodically(() -> {
          sender.sendGauge(cacheSizeMetricName, jaxbContexts.getStorageSize());
          sender.sendGauge(cacheMaxSizeMetricName, contextsMaxCollectionSize);
        }
    );
  }

  @Override
  public Marshaller getContext(Class<?> type) {
    JAXBContext jaxbContext = jaxbContexts.computeIfAbsent(type, clazz -> {
      try {
        return JAXBContext.newInstance(new Class[]{clazz}, Collections.emptyMap());
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
