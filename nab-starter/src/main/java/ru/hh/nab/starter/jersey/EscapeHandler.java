package ru.hh.nab.starter.jersey;

import com.sun.xml.bind.marshaller.CharacterEscapeHandler;

import java.io.IOException;
import java.io.Writer;

/**
 * Copy of {@link com.sun.xml.bind.marshaller.MinimumEscapeHandler}.
 *
 * Escape using <a href="http://www.w3.org/TR/REC-xml/#charsets">xml standard</a> and allow surrogate pairs:
 *
 * <pre>
   Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF] | isLowSurrogate | isHighSurrogate
 *</pre>
 */
public class EscapeHandler implements CharacterEscapeHandler {
  public static final CharacterEscapeHandler INSTANCE = new EscapeHandler();
  private static final char REPLACEMENT_CHARACTER = '\uFFFD';

  private EscapeHandler() {}

  public void escape(char[] ch, int start, int length, boolean isAttVal, Writer out) throws IOException {
    int limit = start + length;
    for (int i = start; i < limit; i++) {
      char c = ch[i];
      boolean validXmlSymbol = isValidXmlSymbol(c);
      if (c == '&' || c == '<' || c == '>' || (c == '\"' && isAttVal) || !validXmlSymbol) {
        if (i != start) {
          out.write(ch, start, i - start);
        }
        start = i + 1;
        switch (ch[i]) {
          case '&':
            out.write("&amp;");
            break;
          case '<':
            out.write("&lt;");
            break;
          case '>':
            out.write("&gt;");
            break;
          case '\"':
            out.write("&quot;");
            break;
        }
        if (!validXmlSymbol) {
          out.write(REPLACEMENT_CHARACTER);
        }
      }
    }

    if (start != limit) {
      out.write(ch, start, limit - start);
    }
  }

  /**
   * The <a href="http://www.w3.org/TR/xml/#charsets">W3C XML reference</a> said that a valid XML char is one of:<br />
   * #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
   * We also include surrogate pairs.
   *
   * @param c The symbol code to test.
   * @return <code>true</code> if <code>c</code> is one of:<br />
   *         #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | isLowSurrogate | isHighSurrogate.
   */
  public static boolean isValidXmlSymbol(char c) {
    return ((c == 0x9) || (c == 0xA) || (c == 0xD) || ((c >= 0x20) && (c <= 0xD7FF)) || ((c >= 0xE000) && (c <= 0xFFFD))
        || ((c >= 0x10000) && (c <= 0x10FFFF))) || Character.isHighSurrogate(c) || Character.isLowSurrogate(c);
  }
}
