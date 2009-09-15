package ru.hh.dxm.structural;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public interface StructureBuildingVisitor<M extends StructureBuilder> {
  boolean visit(M mapping, /* should be value */ XMLStreamReader in) throws XMLStreamException;
}
