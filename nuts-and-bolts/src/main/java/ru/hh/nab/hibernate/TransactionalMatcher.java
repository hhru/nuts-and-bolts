package ru.hh.nab.hibernate;

import com.google.inject.matcher.AbstractMatcher;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Objects;

public class TransactionalMatcher extends AbstractMatcher<AnnotatedElement> {
  private final Class<? extends Annotation> ann;

  public TransactionalMatcher(Class<? extends Annotation> ann) {
    Objects.requireNonNull(ann);
    this.ann = ann;
  }

  @Override
  public boolean matches(AnnotatedElement annotatedElement) {
    Transactional txAnn = annotatedElement.getAnnotation(Transactional.class);
    return txAnn != null && ann.equals(txAnn.value());
  }
}
