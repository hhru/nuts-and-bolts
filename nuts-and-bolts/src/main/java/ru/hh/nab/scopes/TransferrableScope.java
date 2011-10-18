package ru.hh.nab.scopes;

import com.google.inject.Scope;

public interface TransferrableScope extends Scope {
  ScopeClosure capture();
}
