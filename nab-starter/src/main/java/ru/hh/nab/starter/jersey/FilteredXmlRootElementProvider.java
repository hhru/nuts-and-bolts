package ru.hh.nab.starter.jersey;

import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.jaxb.internal.AbstractRootElementJaxbProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Providers;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.sax.SAXSource;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class FilteredXmlRootElementProvider extends AbstractRootElementJaxbProvider {

  @Context
  private Factory<SAXParserFactory> spf;

  public FilteredXmlRootElementProvider(Providers providers) {
    super(providers);
  }

  public FilteredXmlRootElementProvider(Providers providers, MediaType resolverMediaType) {
    super(providers, resolverMediaType);
  }

  @Produces(MediaType.APPLICATION_XML)
  @Consumes(MediaType.APPLICATION_XML)
  public static final class App extends FilteredXmlRootElementProvider {

    public App(@Context Providers providers) {
      super(providers, MediaType.APPLICATION_XML_TYPE);
    }
  }

  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.TEXT_XML)
  public static final class Text extends FilteredXmlRootElementProvider {

    public Text(@Context Providers providers) {
      super(providers, MediaType.TEXT_XML_TYPE);
    }
  }

  @Produces(MediaType.WILDCARD)
  @Consumes(MediaType.WILDCARD)
  public static final class General extends FilteredXmlRootElementProvider {

    public General(@Context Providers providers) {
      super(providers);
    }

    @Override
    protected boolean isSupported(MediaType m) {
      return m.getSubtype().endsWith("+xml");
    }
  }

  @Override
  protected void writeTo(Object t, MediaType mediaType, Charset c,
                         Marshaller m, OutputStream entityStream) throws JAXBException {
    try (FilteredXMLStreamWriter filteredXMLStreamWriter = new FilteredXMLStreamWriter(entityStream)) {
      m.marshal(t, filteredXMLStreamWriter);
      filteredXMLStreamWriter.flush();
    } catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  @Override
  protected Object readFrom(Class<Object> type, MediaType mediaType,
                            Unmarshaller u, InputStream entityStream) throws JAXBException {
    final SAXSource s = getSAXSource(spf.provide(), entityStream);
    if (type.isAnnotationPresent(XmlRootElement.class)) {
      return u.unmarshal(s);
    } else {
      return u.unmarshal(s, type).getValue();
    }
  }

}
