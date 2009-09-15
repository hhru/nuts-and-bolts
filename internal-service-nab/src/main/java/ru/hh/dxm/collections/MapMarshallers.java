package ru.hh.dxm.collections;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public final class MapMarshallers {
  private MapMarshallers() {
  }

  public static AbstractMapMarshaller<String, String> stringMapMarshaller() {
    return stringMapMarshaller("map", "entry", "key");
  }

  public static AbstractMapMarshaller<String, String> stringMapMarshaller(String wrapper, String element, String key) {
    return new AbstractMapMarshaller<String, String>(wrapper, element, key) {
      @Override
      protected String keyFromString(String k) {
        return k;
      }

      @Override
      protected String acceptElementBody(XMLStreamReader in) throws XMLStreamException {
        return in.getElementText();
      }

      @Override
      protected String keyToString(String key) {
        return key;
      }

      @Override
      protected void elementBody(String v, XMLStreamWriter out) throws XMLStreamException {
        out.writeCharacters(v);
      }
    };
  }
}
