package ru.hh.nab.starter.jersey;

import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.jaxb.internal.AbstractCollectionJaxbProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Providers;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collection;

public class FilteredXmlListElementProvider extends AbstractCollectionJaxbProvider {

  @Context
  private Factory<XMLInputFactory> xif;

  public FilteredXmlListElementProvider(Providers ps) {
    super(ps);
  }

  public FilteredXmlListElementProvider(Providers ps, MediaType mt) {
    super(ps, mt);
  }

  @Produces(MediaType.APPLICATION_XML)
  @Consumes(MediaType.APPLICATION_XML)
  public static final class App extends FilteredXmlListElementProvider {

    public App(@Context Providers ps) {
      super(ps , MediaType.APPLICATION_XML_TYPE);
    }
  }

  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.TEXT_XML)
  public static final class Text extends FilteredXmlListElementProvider {

    public Text(@Context Providers ps) {
      super(ps , MediaType.TEXT_XML_TYPE);
    }
  }

  @Produces(MediaType.WILDCARD)
  @Consumes(MediaType.WILDCARD)
  public static final class General extends FilteredXmlListElementProvider {

    public General(@Context Providers ps) {
      super(ps);
    }

    @Override
    protected boolean isSupported(MediaType m) {
      return m.getSubtype().endsWith("+xml");
    }
  }

  @Override
  protected final XMLStreamReader getXMLStreamReader(Class<?> elementType, MediaType mediaType,
                                                     Unmarshaller u, InputStream entityStream) throws XMLStreamException {
    return xif.provide().createXMLStreamReader(entityStream);
  }

  @Override
  public final void writeCollection(Class<?> elementType, Collection<?> t, MediaType mediaType, Charset c,
                                    Marshaller m, OutputStream entityStream) throws JAXBException, IOException {
    final String rootElement = getRootElementName(elementType);
    final String cName = c.name();

    entityStream.write(
      String.format("<?xml version=\"1.0\" encoding=\"%s\" standalone=\"yes\"?>", cName).getBytes(cName));
    entityStream.write(String.format("<%s>", rootElement).getBytes(cName));

    try (FilteredXMLStreamWriter filteredXMLStreamWriter = new FilteredXMLStreamWriter(entityStream)) {
      for (Object o : t) {
        m.marshal(o, filteredXMLStreamWriter);
        filteredXMLStreamWriter.flush();
      }
    } catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
    entityStream.write(String.format("</%s>", rootElement).getBytes(cName));
  }
}
