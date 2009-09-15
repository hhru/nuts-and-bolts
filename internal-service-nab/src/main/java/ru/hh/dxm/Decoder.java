package ru.hh.dxm;

import javax.annotation.concurrent.ThreadSafe;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

@ThreadSafe
public interface Decoder<T> {
  T unmarshal(XMLStreamReader in) throws XMLStreamException;
}
