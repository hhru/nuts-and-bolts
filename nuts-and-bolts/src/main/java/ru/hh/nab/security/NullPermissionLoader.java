package ru.hh.nab.security;

import com.google.common.collect.Lists;

public class NullPermissionLoader implements PermissionLoader {
  private static final Permissions EMPTY_PEMISSIONS =
          new Permissions(Lists.<String>newArrayList());

  @Override
  public Permissions forKey(String key) {
    return EMPTY_PEMISSIONS;
  }

  @Override
  public Permissions anonymous() {
    return EMPTY_PEMISSIONS;
  }
}
