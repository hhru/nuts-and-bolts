package ru.hh.dxm.collections;

import com.google.common.base.Function;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import ru.hh.dxm.Marshaller;

public final class Marshallers {
  public static <ORIG, MARSHALLABLE> Marshaller<ORIG> compose(final Function<ORIG, MARSHALLABLE> fun,
                                                              final Function<MARSHALLABLE, ORIG> reciprocal,
                                                              final Marshaller<MARSHALLABLE> m) {
    return new Marshaller<ORIG>() {
      @Override
      public void marshal(ORIG v, XMLStreamWriter out) throws XMLStreamException {
        m.marshal(fun.apply(v), out);
      }

      @Override
      public ORIG unmarshal(XMLStreamReader in) throws XMLStreamException {
        return reciprocal.apply(m.unmarshal(in));
      }

      @Override
      public String topElement() {
        return m.topElement();
      }

      @Override
      public char[] topElementAsChars() {
        return m.topElementAsChars();
      }
    };
  }

  public static <WHATEVER> Marshaller<WHATEVER> noop() {
    return new Marshaller<WHATEVER>() {
      @Override
      public void marshal(Object v, XMLStreamWriter out) throws XMLStreamException {
      }

      @Override
      public WHATEVER unmarshal(XMLStreamReader in) throws XMLStreamException {
        return null;
      }

      @Override
      public String topElement() {
        return "";
      }

      @Override
      public char[] topElementAsChars() {
        return new char[0];
      }
    };
  }
}
