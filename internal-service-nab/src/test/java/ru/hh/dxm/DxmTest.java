package ru.hh.dxm;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.google.common.collect.ImmutableMap;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.junit.Assert;
import org.junit.Test;
import ru.hh.dxm.collections.MapMarshallers;
import ru.hh.dxm.converters.ValueConverters;

public class DxmTest {
  private static enum MyEnum {
    V1,
    VALUE_TWO
  }

  @Test
  public void testMapMarshalling() throws XMLStreamException {
    final MarshallersRegistry registry = new RegistryBuilder()
            .with(Map.class, MapMarshallers.stringMapMarshaller("map", "entry", "key")).build();

    String xml = marshal(registry, Map.class, ImmutableMap.of("12", "blabla"));
    Assert.assertEquals("<map><entry key=\"12\">blabla</entry></map>", xml);
  }

  @Test
  public void testUnwrappedMapMarshalling() throws XMLStreamException {
    final MarshallersRegistry registry = new RegistryBuilder()
            .with(Map.class, MapMarshallers.stringMapMarshaller(null, "entry", "key")).build();

    String xml = marshal(registry, Map.class, ImmutableMap.of("12", "blabla"));
    Assert.assertEquals("<entry key=\"12\">blabla</entry>", xml);
  }

  @Test
  public void testMapUnmarshalling() throws XMLStreamException {
    final MarshallersRegistry registry = new RegistryBuilder()
            .with(Map.class, MapMarshallers.stringMapMarshaller("map", "entry", "key")).build();

    Map obj = unmarshal("<map><entry key=\"12\">blabla</entry></map>", registry, Map.class);
    Assert.assertEquals(ImmutableMap.of("12", "blabla"), obj);
  }

  @Test
  public void testUnwrappedMapUnmarshalling() throws XMLStreamException {
    final MarshallersRegistry registry = new RegistryBuilder()
            .with(Map.class, MapMarshallers.stringMapMarshaller(null, "entry", "key")).build();

    Map obj = unmarshal("<entry key=\"12\">blabla</entry>", registry, Map.class);
    Assert.assertEquals(ImmutableMap.of("12", "blabla"), obj);
  }

  @Test
  public void testIntMarshaller() throws XMLStreamException {
    final MarshallersRegistry registry = new RegistryBuilder()
            .with(Integer.class, SingleElementMarshaller.of("value", ValueConverters.forInt())).build();

    String xml = marshal(registry, Integer.class, 42);
    Assert.assertEquals("<value>42</value>", xml);
  }

  @Test
  public void testIntUnmarshaller() throws XMLStreamException {
    final MarshallersRegistry registry = new RegistryBuilder()
            .with(Integer.class, SingleElementMarshaller.of("value", ValueConverters.forInt())).build();

    int v = unmarshal("<value>42</value>", registry, Integer.class);
    Assert.assertEquals(42, v);
  }

  @Test
  public void marshalEnum() throws XMLStreamException {
    final MarshallersRegistry registry = new RegistryBuilder()
            .with(MyEnum.class, SingleElementMarshaller.of("enum", ValueConverters.forEnum(MyEnum.class))).build();

    String xml = marshal(registry, MyEnum.class, MyEnum.VALUE_TWO);
    Assert.assertEquals("<enum>VALUE_TWO</enum>", xml);
  }

  @Test
  public void unmarshalEnum() throws XMLStreamException {
    final MarshallersRegistry registry = new RegistryBuilder()
            .with(MyEnum.class, SingleElementMarshaller.of("enum", ValueConverters.forEnum(MyEnum.class))).build();

    MyEnum v = unmarshal("<enum>VALUE_TWO</enum>", registry, MyEnum.class);
    Assert.assertEquals(MyEnum.VALUE_TWO, v);
  }

  @Test
  public void unmarshalEmptyTag() throws XMLStreamException {
    final MarshallersRegistry registry = new RegistryBuilder()
            .with(String.class, SingleElementMarshaller.of("value", ValueConverters.forString())).build();

    String v = unmarshal("<value />", registry, String.class);
    Assert.assertEquals("", v);
  }

  public static XMLStreamReader xml(String s) throws XMLStreamException {
    return new WstxInputFactory().createXMLStreamReader(new StringReader(s));
  }

  public static <T> String marshal(MarshallersRegistry registry, Class<T> klass, T o) throws XMLStreamException {
    StringWriter str = new StringWriter();
    XMLStreamWriter out = new WstxOutputFactory().createXMLStreamWriter(str);
    registry.marshal(klass, o, out);
    out.flush();
    return str.toString();
  }

  public static <T> T unmarshal(String s, MarshallersRegistry registry, Class<T> klass) throws XMLStreamException {
    XMLStreamReader in = xml("<xml>" + s + "</xml>");
    in.next();
    in.require(XMLEvent.START_ELEMENT, null, "xml");
    in.next();
    T ret = registry.unmarshal(klass, in);
    in.require(XMLEvent.END_ELEMENT, null, "xml");
    return ret;
  }
}
