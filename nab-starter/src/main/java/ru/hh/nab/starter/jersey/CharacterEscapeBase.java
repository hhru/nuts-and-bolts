package ru.hh.nab.starter.jersey;

final class CharacterEscapeBase {
  private CharacterEscapeBase() {}

  static final char REPLACEMENT_CHAR = '\uFFFD';

  static boolean isInvalidTextSymbol(char c) {
    return (c < 0x20 && c != 0x9 && c != 0xA && c != 0xD) || (c == 0xFFFE || c == 0xFFFF);
  }
}
