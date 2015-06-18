package ru.hh.nab.scopes;

import com.google.inject.Scope;

public class Scopes {
  public final Scope threadLocal = ThreadLocalScope.THREAD_LOCAL;
}
