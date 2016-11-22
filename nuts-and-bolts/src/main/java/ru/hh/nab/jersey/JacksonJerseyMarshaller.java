package ru.hh.nab.jersey;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.core.provider.AbstractMessageReaderWriterProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

@Produces(MediaType.APPLICATION_JSON)
@Provider
@Singleton
public class JacksonJerseyMarshaller extends AbstractMessageReaderWriterProvider<Object> {
  private final JsonFactory jackson = new JsonFactory(new ObjectMapper());

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return false;
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return type.isAnnotationPresent(JsonModel.class);
  }

  @Override
  public Object readFrom(
      Class<Object> aClass, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> map, InputStream stream)
    throws IOException, WebApplicationException {
    throw new UnsupportedOperationException();
  }

  @Override
  @SuppressWarnings({ "unchecked" })
  public void writeTo(
      Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
      OutputStream entityStream) throws IOException, WebApplicationException {
    try (JsonGenerator g = jackson.createGenerator(entityStream, JsonEncoding.UTF8)) {
      g.writeObject(o);
    }
  }
}
