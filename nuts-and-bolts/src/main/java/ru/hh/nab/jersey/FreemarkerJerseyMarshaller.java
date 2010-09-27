package ru.hh.nab.jersey;


import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.core.provider.AbstractMessageReaderWriterProvider;
import freemarker.cache.ClassTemplateLoader;
import freemarker.core.Environment;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.SimpleMapModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
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
  private final String defaultLayout;
  private BeansWrapper beansWrapper;

  @Inject
  public FreemarkerJerseyMarshaller(@Named("defaultFreeMarkerLayout") String defaultLayout) {
    this.defaultLayout = defaultLayout;
    this.freemarker = new Configuration();
    freemarker.setTemplateLoader(new ClassTemplateLoader(FreemarkerJerseyMarshaller.class, "/freemarker"));
    freemarker.setTemplateUpdateDelay(1);
    freemarker.setNumberFormat("0");
    freemarker.setLocalizedLookup(false);
    freemarker.setDefaultEncoding("UTF-8");
    freemarker.setStrictSyntaxMode(true);
    freemarker.setTagSyntax(Configuration.ANGLE_BRACKET_TAG_SYNTAX);
    freemarker.setWhitespaceStripping(true);
    freemarker.setOutputEncoding("UTF-8");

    beansWrapper = new BeansWrapper();
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
    return type.equals(FreemarkerModel.class);
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
  public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations,
                      MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                      OutputStream entityStream)
          throws IOException, WebApplicationException {
    String encoding = getCharsetAsString(mediaType);

    marshal((FreemarkerModel)o, encoding, new OutputStreamWriter(entityStream, encoding));
  }

  public void marshal(FreemarkerModel model, String encoding, Writer out) throws IOException {
    FreemarkerTemplate ann = Preconditions.checkNotNull(model.annotation);
        
    try {
      String layout = (("".equals(ann.layout())) ? defaultLayout : ann.layout()) + ".ftl";
      Template layoutTemplate = freemarker.getTemplate(layout, encoding);
      Environment pe = layoutTemplate.createProcessingEnvironment(model.appModel, out);
      pe.setGlobalVariable("template", new SimpleScalar(ann.value() + ".ftl"));
      pe.setGlobalVariable("req", new SimpleMapModel(model.requestProperties, beansWrapper));
      pe.process();
    } catch (TemplateException e) {
      throw new IOException("Error marshalling object of class " + model.appModel.getClass().getName(), e);
    }
  }

  @SuppressWarnings("UnusedDeclaration")
  public String marshal(Object o, String encoding) throws IOException {
    StringWriter writer = new StringWriter();
    marshal(FreemarkerModel.of(o), encoding, writer);
    return writer.toString();
  }
}