package ru.hh.dxm.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import ru.hh.dxm.Marshaller;

public abstract class AbstractMapMarshaller<K, V> extends
        AbstractSequenceMarshaller<Map<K, V>, Map.Entry<K, V>, Collection<Map.Entry<K, V>>, Map<K, V>> implements
        Marshaller<Map<K, V>> {
  private final String wrapperElement;
  private final String entryElement;
  private final String keyAttribute;

  protected AbstractMapMarshaller(String wrapperElement, String entryElement, String keyAttribute) {
    super((wrapperElement != null) ? wrapperElement : entryElement);
    this.wrapperElement = wrapperElement;
    this.entryElement = entryElement;
    this.keyAttribute = keyAttribute;
  }

  @Override
  protected final Map<K, V> acceptStartAndMakeBuilder(XMLStreamReader in) throws XMLStreamException {
    assert in.isStartElement();
    assert topElement().equals(in.getLocalName());
    if (wrapperElement != null)
      in.next();
    return new HashMap<K, V>();
  }

  @Override
  protected final boolean acceptAndAddElement(Map<K, V> builder, XMLStreamReader in) throws XMLStreamException {
    if (!in.isStartElement() || !entryElement.equals(in.getName().getLocalPart()))
      return false;
    K k = keyFromString(in.getAttributeValue(null, keyAttribute));
    V v = acceptElementBody(in);
    builder.put(k, v);
    return true;
  }

  protected abstract K keyFromString(String k);

  protected abstract V acceptElementBody(XMLStreamReader in) throws XMLStreamException;

  @Override
  protected final Map<K, V> acceptEndAndBuild(Map<K, V> builder, XMLStreamReader in) throws XMLStreamException {
    if (wrapperElement != null)
      in.next();
    return builder;
  }

  @SuppressWarnings({"unchecked"})
  protected final Collection<Map.Entry<K, V>> sequence(Map<K, V> v) {
    return v.entrySet();
  }

  protected final void start(Collection<Entry<K, V>> sequence, XMLStreamWriter out)
          throws XMLStreamException {
    if (wrapperElement != null)
      out.writeStartElement(wrapperElement);
  }

  protected final void element(Entry<K, V> entry, XMLStreamWriter out) throws XMLStreamException {
    out.writeStartElement(entryElement);
    out.writeAttribute(keyAttribute, keyToString(entry.getKey()));
    elementBody(entry.getValue(), out);
    out.writeEndElement();
  }

  protected abstract String keyToString(K key);

  protected abstract void elementBody(V v, XMLStreamWriter out) throws XMLStreamException;

  protected final void end(Collection<Entry<K, V>> sequence, XMLStreamWriter out)
          throws XMLStreamException {
    if (wrapperElement != null)
      out.writeEndElement();
  }
}
