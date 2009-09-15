package ru.hh.dxm.collections;

import com.google.common.collect.Lists;
import java.util.Collection;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import ru.hh.dxm.converters.ValueConverter;
import ru.hh.dxm.converters.ValueConverters;

public final class CollectionMarshallers {
  private CollectionMarshallers() {
  }

  public static <V> AbstractCollectionMarshaller<V, Collection<V>> of(final String wrapper, final String element,
                                                                      final ValueConverter<V> converter) {
    return new AbstractCollectionMarshaller<V, Collection<V>>(wrapper, element) {
      @Override
      protected Collection<V> newCollection() {
        return Lists.newArrayList();
      }

      @Override
      protected V acceptElementBody(XMLStreamReader in) throws XMLStreamException {
        return converter.fromString(in.getText());
      }

      @Override
      protected void elementBody(V v, XMLStreamWriter out) throws XMLStreamException {
        out.writeCharacters(converter.toString(v));
      }
    };
  }

  public static AbstractCollectionMarshaller<String, Collection<String>> ofStrings(
          String wrapper, String element) {
    return of(wrapper, element, ValueConverters.forString());
  }
}
