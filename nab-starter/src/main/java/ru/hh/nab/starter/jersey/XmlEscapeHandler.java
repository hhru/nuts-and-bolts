package ru.hh.nab.starter.jersey;

import com.sun.xml.bind.marshaller.CharacterEscapeHandler;

import java.io.IOException;
import java.io.Writer;

/**
 * Copy of {@link com.sun.xml.bind.marshaller.MinimumEscapeHandler}.
 * Also replaces invalid text symbols.
 */
public class XmlEscapeHandler implements CharacterEscapeHandler {
  public static final CharacterEscapeHandler INSTANCE = new XmlEscapeHandler();

  private XmlEscapeHandler() {}

  public void escape(char[] ch, int start, int length, boolean isAttVal, Writer out) throws IOException {
    int limit = start + length;
    for (int i = start; i < limit; i++) {
      char c = ch[i];
      if (c == '&' || c == '<' || c == '>' || c == '\r' || (c == '\n' && isAttVal) || (c == '\"' && isAttVal) ||
          CharacterEscapeBase.isInvalidTextSymbol(c)) {

        if (i != start) {
          out.write(ch, start, i - start);
        }
        start = i + 1;
        switch (c) {
          case '&':
            out.write("&amp;");
            break;
          case '<':
            out.write("&lt;");
            break;
          case '>':
            out.write("&gt;");
            break;
          case '\n':
          case '\r':
            out.write("&#");
            out.write(Integer.toString(c));
            out.write(';');
            break;
          case '\"':
            out.write("&quot;");
            break;
          default:
            out.write(CharacterEscapeBase.REPLACEMENT_CHAR);
            break;
        }
      }
    }

    if (start != limit) {
      out.write(ch, start, limit - start);
    }
  }
}
