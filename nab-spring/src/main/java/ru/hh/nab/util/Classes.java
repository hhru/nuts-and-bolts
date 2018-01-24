package ru.hh.nab.util;

public abstract class Classes {
  public static boolean hasDeclaredMethod(Class<?> klass, String method, Class<?>... argKlasses) {
    try {
      klass.getDeclaredMethod(method, argKlasses);
      return true;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }
}
