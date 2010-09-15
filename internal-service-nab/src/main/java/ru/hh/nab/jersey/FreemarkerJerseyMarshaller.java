package ru.hh.nab.jersey;


import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import com.sun.jersey.core.provider.AbstractMessageReaderWriterProvider;
import freemarker.cache.ClassTemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

@Produces(MediaType.WILDCARD)
@Provider
@Singleton
public class FreemarkerJerseyMarshaller extends AbstractMessageReaderWriterProvider<Object> {
  private final Configuration freemarker;
  private static final String DEFAULT_ENCODING = "UTF-8";

  public FreemarkerJerseyMarshaller() {
    this.freemarker = new Configuration();
    freemarker.setTemplateLoader(new ClassTemplateLoader(FreemarkerJerseyMarshaller.class, "/freemarker"));
    freemarker.setTemplateUpdateDelay(1);
    freemarker.setNumberFormat("0");
    freemarker.setLocalizedLookup(false);
    freemarker.setDefaultEncoding("UTF-8");
    freemarker.setStrictSyntaxMode(true);
    freemarker.setTagSyntax(Configuration.ANGLE_BRACKET_TAG_SYNTAX);
    freemarker.setWhitespaceStripping(true);

    BeansWrapper beansWrapper = new BeansWrapper();
    beansWrapper.setExposeFields(true);
    beansWrapper.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
    beansWrapper.setStrict(true);
    beansWrapper.setSimpleMapWrapper(true);
    freemarker.setObjectWrapper(beansWrapper);
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

    marshal(o, encoding, new OutputStreamWriter(stream, encoding));
  }

  public void marshal(Object o, String encoding, Writer out) throws IOException {
    FreemarkerTemplate ann = o.getClass().getAnnotation(FreemarkerTemplate.class);
    Preconditions.checkNotNull(ann);
    try {
      freemarker.getTemplate(ann.value() + ".ftl", encoding).process(o, out);
    } catch (TemplateException e) {
      throw new IOException("Error marshalling object of class " + o.getClass().getName(), e);
    }
  }

  public String marshal(Object o, String encoding) throws IOException {
    StringWriter writer = new StringWriter();
    marshal(o, encoding, writer);
    return writer.toString();
  }
}