package ru.hh.dxm.structural;

import java.lang.reflect.Method;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import org.junit.Assert;
import org.junit.Test;
import ru.hh.dxm.DxmTest;
import ru.hh.dxm.SingleElementMarshaller;
import ru.hh.dxm.converters.ValueConverters;
import ru.hh.dxm.structural.Mapping.MappedElementDefinition;

public class MappingTest {
  @SuppressWarnings({"unchecked"})
  @Test
  public void simpleTest() throws NoSuchMethodException, XMLStreamException {
    Method factory = ExampleObject.class.getDeclaredMethod("newInstance", int.class, String.class, boolean.class);

    MappedElementDefinition[] defs = new MappedElementDefinition[3];
    defs[0] = new MappedElementDefinition(Integer.class, new String[]{"doc", "a"}, SingleElementMarshaller.of("a",
            ValueConverters.forInt()));
    defs[1] = new MappedElementDefinition(String.class, new String[]{"doc", "b"}, SingleElementMarshaller.of("b",
            ValueConverters.forString()));
    defs[2] = new MappedElementDefinition(Boolean.class, new String[]{"doc", "c"}, SingleElementMarshaller.of("c",
            ValueConverters.forBool()));
    Mapping mapping = new Mapping(defs, factory);

    StructuredModelDecoder decoder = mapping.getDecoder();

    XMLStreamReader in = DxmTest.xml("<xml><doc><a>12</a><b>blabla</b><c>false</c></doc></xml>");
    in.next();
    in.next();
    in.require(XMLEvent.START_ELEMENT, null, "doc");

    ExampleObject ret = (ExampleObject) decoder.unmarshal(in);

    Assert.assertEquals(12, ret.intProperty);
    Assert.assertEquals("blabla", ret.stringProperty);
    Assert.assertEquals(false, ret.boolProperty);
  }

  public static class ExampleObject {
    public final int intProperty;
    public final String stringProperty;
    public final boolean boolProperty;

    public ExampleObject(int intProperty, String stringProperty, boolean boolProperty) {
      this.intProperty = intProperty;
      this.stringProperty = stringProperty;
      this.boolProperty = boolProperty;
    }

    public static ExampleObject newInstance(int intProperty, String stringProperty, boolean boolProperty) {
      return new ExampleObject(intProperty, stringProperty, boolProperty);
    }
  }
}
