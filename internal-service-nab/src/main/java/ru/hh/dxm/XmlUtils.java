package ru.hh.dxm;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

public final class XmlUtils {
  private XmlUtils() {
  }

  /**
   * <doc><e1><e2/></e1><e3/></doc>
   * ^-------->^
   * ^----------------------->^
   * ^^
   *
   * @param in stream to read from, MUST be in START_ELEMENT state
   * @throws XMLStreamException
   */
  public static void skipCurrentSubTree(XMLStreamReader in) throws XMLStreamException {
    assert in.isStartElement();
    int depth = 1;
    do {
      int event = in.next();
      switch (event) {
        case XMLEvent.START_ELEMENT:
          ++depth;
          break;
        case XMLEvent.END_ELEMENT:
          --depth;
          break;
      }
    } while (depth != 0);
  }
}
