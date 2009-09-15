package ru.hh.nab;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.google.inject.Inject;
import com.sun.jersey.core.provider.AbstractMessageReaderWriterProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import ru.hh.dxm.MarshallersRegistry;

@Produces({"application/xml", "text/xml", "application/*+xml"})
@Consumes({"application/xml", "text/xml", "application/*+xml"})
@Provider
public class JerseyDXMarshaller extends AbstractMessageReaderWriterProvider<Object> {
  private static final String DEFAULT_ENCODING = "UTF-8";
  private final MarshallersRegistry dxm;
  private final WstxInputFactory inputFactory = new WstxInputFactory();
  private final WstxOutputFactory outputFactory = new WstxOutputFactory();

  @Inject
  public JerseyDXMarshaller(MarshallersRegistry dxm) {
    this.dxm = dxm;
  }

  public boolean isReadable(Class<?> type, Type genericType, Annotation annotations[], MediaType mediaType) {
    return dxm.marshaller(type) != null;
  }

  public boolean isWriteable(Class<?> type, Type genericType, Annotation annotations[], MediaType mediaType) {
    return dxm.marshaller(type) != null;
  }

  protected static String getCharsetAsString(MediaType m) {
    if (m == null) {
      return DEFAULT_ENCODING;
    }
    String result = m.getParameters().get("charset");
    return (result == null) ? DEFAULT_ENCODING : result;
  }

  public Object readFrom(Class<Object> aClass, Type genericType, Annotation[] annotations,
                         MediaType mediaType, MultivaluedMap<String, String> map, InputStream stream)
          throws IOException, WebApplicationException {
    String encoding = getCharsetAsString(mediaType);
    try {
      XMLStreamReader input = inputFactory.createXMLStreamReader(stream, encoding);
      return dxm.unmarshal(aClass, input);
    } catch (XMLStreamException e) {
      throw new WebApplicationException(Status.BAD_REQUEST);
    }
  }

  @SuppressWarnings({"unchecked"})
  public void writeTo(Object o, Class<?> aClass, Type type, Annotation[] annotations,
                      MediaType mediaType, MultivaluedMap<String, Object> map, OutputStream stream)
          throws IOException, WebApplicationException {
    String encoding = getCharsetAsString(mediaType);
    try {
      XMLStreamWriter out = outputFactory.createXMLStreamWriter(stream, encoding);
      dxm.marshal((Class) aClass, o, out);
      out.close();
    } catch (XMLStreamException e) {
      throw new IOException("Error marshalling object of class " + aClass.getName(), e);
    }
  }
}