package ru.hh.dxm;

import javax.annotation.concurrent.Immutable;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import ru.hh.dxm.converters.ValueConverter;

@Immutable
public final class SingleElementMarshaller<T> extends BaseDetectable implements Marshaller<T> {
  private final ValueConverter<T> converter;

  public SingleElementMarshaller(String elementName, ValueConverter<T> converter) {
    super(elementName);
    this.converter = converter;
  }

  @Override
  public void marshal(T v, XMLStreamWriter out) throws XMLStreamException {
    if (v == null)
      return;
    out.writeStartElement(topElement());
    out.writeCharacters(converter.toString(v));
    out.writeEndElement();
  }

  @Override
  public T unmarshal(XMLStreamReader in) throws XMLStreamException {
    assert in.isStartElement();
    assert topElement().equals(in.getLocalName());
    T ret = converter.fromString(in.getElementText());
    in.next();
    return ret;
  }

  public static <T> SingleElementMarshaller<T> of(String elementName, ValueConverter<T> converter) {
    return new SingleElementMarshaller<T>(elementName, converter);
  }
}
