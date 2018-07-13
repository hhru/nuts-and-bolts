package ru.hh.nab.starter.jersey;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;

public class FilteredXMLStreamWriter implements XMLStreamWriter, AutoCloseable {

  private static final char REPLACEMENT_CHARACTER = '\uFFFD';

  private final XMLStreamWriter writer;

  public FilteredXMLStreamWriter(OutputStream outputStream) throws XMLStreamException {
    if (outputStream == null) {
      throw new IllegalArgumentException();
    }
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    this.writer = outputFactory.createXMLStreamWriter(outputStream);
  }

  public FilteredXMLStreamWriter(XMLStreamWriter writer) {
    if (writer == null) {
      throw new IllegalArgumentException();
    }
    this.writer = writer;
  }

  /**
   * http://www.w3.org/TR/2006/REC-xml-20060816/#charsets
   */
  private String sanitize(String string) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < string.length(); i++) {
      char current = string.charAt(i);
      if ((current == 0x9) ||
          (current == 0xA) ||
          (current == 0xD) ||
          ((current >= 0x20) && (current <= 0xD7FF)) ||
          ((current >= 0xE000) && (current <= 0xFFFD)) ||
          ((current >= 0x10000) && (current <= 0x10FFFF)) ||
          Character.isHighSurrogate(current) ||
          Character.isLowSurrogate(current)) {
        builder.append(current);
      } else {
        builder.append(REPLACEMENT_CHARACTER);
      }
    }
    return builder.toString();
  }

  @Override
  public void writeStartElement(String localName) throws XMLStreamException {
    writer.writeStartElement(localName);
  }

  @Override
  public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
    writer.writeStartElement(namespaceURI, localName);
  }

  @Override
  public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
    writer.writeStartElement(prefix, localName, namespaceURI);
  }

  @Override
  public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
    writer.writeEmptyElement(namespaceURI, localName);
  }

  @Override
  public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
    writer.writeEmptyElement(prefix, localName, namespaceURI);
  }

  @Override
  public void writeEmptyElement(String localName) throws XMLStreamException {
    writer.writeEmptyElement(localName);
  }

  @Override
  public void writeEndElement() throws XMLStreamException {
    writer.writeEndElement();
  }

  @Override
  public void writeEndDocument() throws XMLStreamException {
    writer.writeEndDocument();
  }

  @Override
  public void close() throws XMLStreamException {
    writer.close();
  }

  @Override
  public void flush() throws XMLStreamException {
    writer.flush();
  }

  @Override
  public void writeAttribute(String localName, String value) throws XMLStreamException {
    writer.writeAttribute(localName, sanitize(value));
  }

  @Override
  public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
    writer.writeAttribute(prefix, namespaceURI, localName, sanitize(value));
  }

  @Override
  public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
    writer.writeAttribute(namespaceURI, localName, sanitize(value));
  }

  @Override
  public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
    writer.writeNamespace(prefix, namespaceURI);
  }

  @Override
  public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
    writer.writeDefaultNamespace(namespaceURI);
  }

  @Override
  public void writeComment(String data) throws XMLStreamException {
    writer.writeComment(data);
  }

  @Override
  public void writeProcessingInstruction(String target) throws XMLStreamException {
    writer.writeProcessingInstruction(target);
  }

  @Override
  public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
    writer.writeProcessingInstruction(target, data);
  }

  @Override
  public void writeCData(String data) throws XMLStreamException {
    writer.writeCData(sanitize(data));
  }

  @Override
  public void writeDTD(String dtd) throws XMLStreamException {
    writer.writeDTD(dtd);
  }

  @Override
  public void writeEntityRef(String name) throws XMLStreamException {
    writer.writeEntityRef(name);
  }

  @Override
  public void writeStartDocument() throws XMLStreamException {
    writer.writeStartDocument();
  }

  @Override
  public void writeStartDocument(String version) throws XMLStreamException {
    writer.writeStartDocument(version);
  }

  @Override
  public void writeStartDocument(String encoding, String version) throws XMLStreamException {
    writer.writeStartDocument(encoding, version);
  }

  @Override
  public void writeCharacters(String text) throws XMLStreamException {
    writer.writeCharacters(sanitize(text));
  }

  @Override
  public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
    writeCharacters(new String(text, start, len));
  }

  @Override
  public String getPrefix(String uri) throws XMLStreamException {
    return writer.getPrefix(uri);
  }

  @Override
  public void setPrefix(String prefix, String uri) throws XMLStreamException {
    writer.setPrefix(prefix, uri);
  }

  @Override
  public void setDefaultNamespace(String uri) throws XMLStreamException {
    writer.setDefaultNamespace(uri);
  }

  @Override
  public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
    writer.setNamespaceContext(context);
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    return writer.getNamespaceContext();
  }

  @Override
  public Object getProperty(String name) throws IllegalArgumentException {
    return writer.getProperty(name);
  }

}
