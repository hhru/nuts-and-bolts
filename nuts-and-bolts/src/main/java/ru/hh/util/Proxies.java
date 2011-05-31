package ru.hh.util;

import net.sf.cglib.proxy.Enhancer;

public abstract class Proxies {
  public static Class<?> realClass(Object o) {
    Class klass = o.getClass();
    while (klass != null && Enhancer.isEnhanced(klass)) {
      klass = klass.getSuperclass();
    }
    return klass;
  }
}
