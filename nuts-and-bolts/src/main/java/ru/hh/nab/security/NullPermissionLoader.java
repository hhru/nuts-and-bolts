package ru.hh.nab.security;

import java.util.Collections;

public class NullPermissionLoader implements PermissionLoader {
  private static final Permissions EMPTY_PEMISSIONS = new Permissions(Collections.emptyList());

  @Override
  public Permissions forKey(String key) {
    return EMPTY_PEMISSIONS;
  }

  @Override
  public Permissions anonymous() {
    return EMPTY_PEMISSIONS;
  }
}
