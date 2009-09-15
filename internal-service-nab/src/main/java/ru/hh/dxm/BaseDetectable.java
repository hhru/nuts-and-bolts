package ru.hh.dxm;

import java.util.Arrays;
import javax.annotation.concurrent.Immutable;

@Immutable
public abstract class BaseDetectable implements Detectable {
  private final String elementName;
  private final char[] elementNameChars;

  protected BaseDetectable(String topElement) {
    this.elementName = topElement;
    this.elementNameChars = topElement.toCharArray();
  }

  @Override
  public final String topElement() {
    return elementName;
  }

  @Override
  public final char[] topElementAsChars() {
    assert Arrays.equals(elementNameChars, elementName.toCharArray());
    return elementNameChars;
  }
}
