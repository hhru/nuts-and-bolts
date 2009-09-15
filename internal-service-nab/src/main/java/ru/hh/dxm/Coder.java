package ru.hh.dxm;

import javax.annotation.concurrent.ThreadSafe;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

@ThreadSafe
public interface Coder<T> {
  void marshal(T v, XMLStreamWriter out) throws XMLStreamException;
}
