package ru.hh.dxm.collections;

import java.util.Collection;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public abstract class AbstractCollectionMarshaller<V, C extends Collection<V>>
        extends AbstractSequenceMarshaller<C, V, C, C> {
  private final String wrapperElement;
  private final String entryElement;

  protected abstract C newCollection();

  protected abstract V acceptElementBody(XMLStreamReader in) throws XMLStreamException;

  protected abstract void elementBody(V v, XMLStreamWriter out) throws XMLStreamException;

  protected AbstractCollectionMarshaller(String wrapperElement, String entryElement) {
    super((wrapperElement != null) ? wrapperElement : entryElement);
    this.wrapperElement = wrapperElement;
    this.entryElement = entryElement;
  }

  @Override
  protected final C acceptStartAndMakeBuilder(XMLStreamReader in) throws XMLStreamException {
    if (wrapperElement != null) {
      in.require(XMLStreamConstants.START_ELEMENT, null, wrapperElement);
      in.nextTag();
    } else {
      in.require(XMLStreamConstants.START_ELEMENT, null, entryElement);
    }
    return newCollection();
  }

  @Override
  protected final boolean acceptAndAddElement(C builder, XMLStreamReader in) throws XMLStreamException {
    if (!in.isStartElement() || !entryElement.equals(in.getName().getLocalPart()))
      return false;
    in.require(XMLStreamConstants.START_ELEMENT, null, entryElement);
    V v = acceptElementBody(in);
    in.next();
    builder.add(v);
    return true;
  }

  @Override
  protected final C acceptEndAndBuild(C builder, XMLStreamReader in) throws XMLStreamException {
    if (wrapperElement != null)
      in.next();
    return builder;
  }

  @Override
  protected final C sequence(C v) {
    return v;
  }

  @Override
  protected final void start(C sequence, XMLStreamWriter out) throws XMLStreamException {
    if (wrapperElement != null)
      out.writeStartElement(wrapperElement);
  }

  @Override
  protected final void element(V entry, XMLStreamWriter out) throws XMLStreamException {
    out.writeStartElement(entryElement);
    elementBody(entry, out);
    out.writeEndElement();
  }

  @Override
  protected final void end(C sequence, XMLStreamWriter out) throws XMLStreamException {
    if (wrapperElement != null)
      out.writeEndElement();
  }
}
