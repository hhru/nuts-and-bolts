package ru.hh.dxm.collections;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import ru.hh.dxm.BaseDetectable;
import ru.hh.dxm.Marshaller;

public abstract class AbstractSequenceMarshaller<T, E, I extends Iterable<E>, B> extends BaseDetectable implements
        Marshaller<T> {
  protected AbstractSequenceMarshaller(String topElement) {
    super(topElement);
  }

  @Override
  public final void marshal(T v, XMLStreamWriter out) throws XMLStreamException {
    if (v == null)
      return;
    I entries = sequence(v);
    start(entries, out);
    for (E elt : entries)
      element(elt, out);
    end(entries, out);
  }

  @Override
  public final T unmarshal(XMLStreamReader in) throws XMLStreamException {
    B builder = acceptStartAndMakeBuilder(in);
    while (acceptAndAddElement(builder, in)) {
      in.next();
    }
    return acceptEndAndBuild(builder, in);
  }

  protected abstract B acceptStartAndMakeBuilder(XMLStreamReader in) throws XMLStreamException;

  protected abstract boolean acceptAndAddElement(B builder, XMLStreamReader in)
          throws XMLStreamException;

  protected abstract T acceptEndAndBuild(B builder, XMLStreamReader in) throws XMLStreamException;

  protected abstract I sequence(T v);

  protected abstract void start(I sequence, XMLStreamWriter out) throws XMLStreamException;

  protected abstract void element(E entry, XMLStreamWriter out) throws XMLStreamException;

  protected abstract void end(I sequence, XMLStreamWriter out) throws XMLStreamException;
}
