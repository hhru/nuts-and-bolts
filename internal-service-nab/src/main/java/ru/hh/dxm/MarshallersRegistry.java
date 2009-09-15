package ru.hh.dxm;

import com.google.common.collect.MapMaker;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class MarshallersRegistry implements Marshallers {
  private final Map<Class<?>, Marshaller<?>> marshallers;
  private final Map<Class<?>, Class<?>> knownSuperclasses = new MapMaker().weakKeys().weakValues().makeMap();

  public MarshallersRegistry(Map<Class<?>, Marshaller<?>> marshallers) {
    this.marshallers = marshallers;
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public <T> Marshaller<T> marshaller(Class<T> klass) {
    klass = findNearestKnownClass(klass);
    if (klass == null)
      return null;
    return (Marshaller<T>) marshallers.get(klass);
  }

  @SuppressWarnings({"unchecked"})
  private Class findNearestKnownClass(Class klass) {
    Class known = knownSuperclasses.get(klass);
    if (known != null)
      return known;
    if (marshallers.containsKey(klass)) {
      knownSuperclasses.put(klass, klass);
      return klass;
    }

    Class sc = klass.getSuperclass();
    if (sc == null)
      return null;

    known = findNearestKnownClass(sc);
    if (known != null) {
      knownSuperclasses.put(klass, known);
      return known;
    }

    for (Class i : klass.getInterfaces()) {
      known = findNearestKnownClass(i);
      if (known != null) {
        knownSuperclasses.put(klass, known);
        return known;
      }
    }

    return known;
  }

  public <T> void marshal(Class<T> klass, T v, XMLStreamWriter out) throws XMLStreamException {
    marshaller(klass).marshal(v, out);
  }

  public <T> T unmarshal(Class<T> c, XMLStreamReader in) throws XMLStreamException {
    if (in.getEventType() == XMLStreamConstants.START_DOCUMENT)
      in.next();
    return marshaller(c).unmarshal(in);
  }
}
