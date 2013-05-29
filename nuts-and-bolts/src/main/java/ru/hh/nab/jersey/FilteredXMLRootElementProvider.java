package ru.hh.nab.jersey;

import com.sun.jersey.core.provider.jaxb.AbstractRootElementProvider;
import com.sun.jersey.spi.inject.Injectable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
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

public class FilteredXMLRootElementProvider extends AbstractRootElementProvider {

  @Context
  private Injectable<SAXParserFactory> spf;

  public FilteredXMLRootElementProvider(Providers providers) {
    super(providers);
  }

  public FilteredXMLRootElementProvider(Providers providers, MediaType mt) {
    super(providers, mt);
  }

  @Produces(MediaType.APPLICATION_XML)
  @Consumes(MediaType.APPLICATION_XML)
  @Provider
  @Singleton
  public static final class App extends FilteredXMLRootElementProvider {

    @Inject
    public App(Providers providers) {
      super(providers, MediaType.APPLICATION_XML_TYPE);
    }
  }

  @Produces(MediaType.TEXT_XML)
  @Consumes(MediaType.TEXT_XML)
  @Provider
  @Singleton
  public static final class Text extends FilteredXMLRootElementProvider {

    @Inject
    public Text(Providers providers) {
      super(providers, MediaType.TEXT_XML_TYPE);
    }
  }

  @Produces(MediaType.WILDCARD)
  @Consumes(MediaType.WILDCARD)
  @Provider
  @Singleton
  public static final class General extends FilteredXMLRootElementProvider {

    @Inject
    public General(Providers providers) {
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
    try {
      FilteredXMLStreamWriter filteredXMLStreamWriter = null;
      try {
        filteredXMLStreamWriter = new FilteredXMLStreamWriter(entityStream);
        m.marshal(t, filteredXMLStreamWriter);
        filteredXMLStreamWriter.flush();
      } finally {
        if(filteredXMLStreamWriter != null) {
          filteredXMLStreamWriter.close();
        }
      }
    } catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  @Override
  protected Object readFrom(Class<Object> type, MediaType mediaType,
                            Unmarshaller u, InputStream entityStream) throws JAXBException {
    final SAXSource s = getSAXSource(spf.getValue(), entityStream);
    if (type.isAnnotationPresent(XmlRootElement.class)) {
      return u.unmarshal(s);
    } else {
      return u.unmarshal(s, type).getValue();
    }
  }

}
