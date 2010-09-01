package ru.hh.nab.scopes;

import com.google.inject.ScopeAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ScopeAnnotation
public @interface ThreadLocalScoped {
}
