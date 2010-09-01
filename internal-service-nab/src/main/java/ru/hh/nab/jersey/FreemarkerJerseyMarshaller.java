package ru.hh.nab.jersey;


import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import com.sun.jersey.core.provider.AbstractMessageReaderWriterProvider;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.apache.commons.beanutils.BeanMap;

@Produces(MediaType.WILDCARD)
@Provider
@Singleton
public class FreemarkerJerseyMarshaller extends AbstractMessageReaderWriterProvider<Object> {
  private final Configuration freemarker;
  private static final String DEFAULT_ENCODING = "UTF-8";

  public FreemarkerJerseyMarshaller() {
    this.freemarker = new Configuration();
    freemarker.setTemplateLoader(new ClassTemplateLoader(FreemarkerJerseyMarshaller.class, "/freemarker"));
    freemarker.setNumberFormat("0");
    freemarker.setLocalizedLookup(false);
    freemarker.setTemplateUpdateDelay(Integer.MAX_VALUE);
    freemarker.setDefaultEncoding("UTF-8");
  }

  public boolean isReadable(Class<?> type, Type genericType, Annotation annotations[], MediaType mediaType) {
    return false;
  }

  public boolean isWriteable(Class<?> type, Type genericType, Annotation annotations[], MediaType mediaType) {
    return type.getAnnotation(FreemarkerTemplate.class) != null;
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
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings({"unchecked"})
  public void writeTo(Object o, Class<?> aClass, Type type, Annotation[] annotations,
                      MediaType mediaType, MultivaluedMap<String, Object> map, OutputStream stream)
          throws IOException, WebApplicationException {
    String encoding = getCharsetAsString(mediaType);
    FreemarkerTemplate ann = aClass.getAnnotation(FreemarkerTemplate.class);
    Preconditions.checkNotNull(ann);

    try {
      freemarker.getTemplate(ann.value() + ".ftl", encoding).process(o, new OutputStreamWriter(stream, encoding));
    } catch (TemplateException e) {
      throw new IOException("Error marshalling object of class " + aClass.getName(), e);
    }
  }
}