package ru.hh.nab.security;

import com.google.inject.matcher.AbstractMatcher;
import java.lang.reflect.AnnotatedElement;

public class SecureMatcher extends AbstractMatcher<AnnotatedElement> {
  @Override
  public boolean matches(AnnotatedElement annotatedElement) {
    return annotatedElement.isAnnotationPresent(Secure.class);
  }
}
