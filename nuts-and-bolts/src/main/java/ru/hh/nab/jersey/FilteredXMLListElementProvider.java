package ru.hh.nab.jersey;

import com.sun.jersey.core.provider.jaxb.AbstractListElementProvider;
import com.sun.jersey.spi.inject.Injectable;
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

public class FilteredXMLListElementProvider extends AbstractListElementProvider {

  @Context
  private Injectable<XMLInputFactory> xif;

  public FilteredXMLListElementProvider(Providers ps) {
    super(ps);
  }

  public FilteredXMLListElementProvider(Providers ps, MediaType mt) {
    super(ps, mt);
  }

  @Produces(MediaType.APPLICATION_XML)
  @Consumes(MediaType.APPLICATION_XML)
  public static final class App extends FilteredXMLListElementProvider {

    public App(@Context Providers ps) {
      super(ps , MediaType.APPLICATION_XML_TYPE);
    }
  }

  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.TEXT_XML)
  public static final class Text extends FilteredXMLListElementProvider {

    public Text(@Context Providers ps) {
      super(ps , MediaType.TEXT_XML_TYPE);
    }
  }

  @Produces(MediaType.WILDCARD)
  @Consumes(MediaType.WILDCARD)
  public static final class General extends FilteredXMLListElementProvider {

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
    return xif.getValue().createXMLStreamReader(entityStream);
  }

  public final void writeList(Class<?> elementType, Collection<?> t, MediaType mediaType, Charset c,
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
