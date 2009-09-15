package ru.hh.dxm;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import org.junit.Test;

public class XmlUtilsTest {
  @Test
  public void skipCurrentSubTree() throws XMLStreamException {
    XMLStreamReader in = DxmTest.xml("<doc><e1><e2/></e1><e3/></doc>");

    in.next();
    in.next();
    in.require(XMLEvent.START_ELEMENT, null, "e1");
    XmlUtils.skipCurrentSubTree(in);
    in.require(XMLEvent.END_ELEMENT, null, "e1");
    in.next();
    in.require(XMLEvent.START_ELEMENT, null, "e3");
    XmlUtils.skipCurrentSubTree(in);
    in.require(XMLEvent.END_ELEMENT, null, "e3");
  }
}
