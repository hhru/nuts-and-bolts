package ru.hh.nab.hibernate;

import java.io.Serializable;
import java.lang.annotation.Annotation;

public class TransactionalImpl implements Transactional, Serializable {
  private final Class<? extends Annotation> module;

  public TransactionalImpl(Class<? extends Annotation> module) {
    this.module = module;
  }

  @Override
  public Class<? extends Annotation> value() {
    return module;
  }

  public int hashCode() {
    return (127 * "value".hashCode()) ^ module.hashCode();
  }

  public boolean equals(Object o) {
    if (!(o instanceof Transactional)) {
      return false;
    }

    Transactional other = (Transactional) o;
    return module.equals(other.value());
  }

  public String toString() {
    return "@" + Transactional.class.getName() +"(value=" + module.getName() + ")";
  }

  @Override
  public boolean readOnly() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean rollback() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return Transactional.class;
  }
}
