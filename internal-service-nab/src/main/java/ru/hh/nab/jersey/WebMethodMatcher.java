package ru.hh.nab.jersey;

import com.google.inject.matcher.AbstractMatcher;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;

public class WebMethodMatcher extends AbstractMatcher<AnnotatedElement> {
  @Override
  public boolean matches(AnnotatedElement annotatedElement) {
    for (Annotation a : annotatedElement.getAnnotations()) {
      if (a instanceof Path)
        return true;
      if (a.annotationType().isAnnotationPresent(HttpMethod.class))
        return true;
    }
    return false;
  }
}
