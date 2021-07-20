package ru.hh.nab.starter.jersey;

import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import java.util.stream.IntStream;

public class JsonCharacterEscapes extends JsonpCharacterEscapes {
  private static final SerializableString REPLACEMENT_STR = new SerializedString(String.valueOf(CharacterEscapeBase.REPLACEMENT_CHAR));

  private static final int[] asciiEscapes = CharacterEscapes.standardAsciiEscapesForJSON();
  static {
    IntStream.range(0, 0x20)
      .filter(i -> CharacterEscapeBase.isInvalidTextSymbol((char) i))
      .forEach(i -> asciiEscapes[i] = CharacterEscapes.ESCAPE_CUSTOM);
  }

  @Override
  public int[] getEscapeCodesForAscii() {
    return asciiEscapes;
  }

  @Override
  public SerializableString getEscapeSequence(int i) {
    if (CharacterEscapeBase.isInvalidTextSymbol((char) i)) {
      return REPLACEMENT_STR;
    }

    return super.getEscapeSequence(i);
  }
}
