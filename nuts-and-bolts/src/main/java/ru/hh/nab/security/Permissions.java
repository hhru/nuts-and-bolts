package ru.hh.nab.security;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public final class Permissions {
  private final Set<String> permissions;

  public Permissions(Iterable<String> permissions) {
    this.permissions = ImmutableSet.copyOf(permissions);
  }

  public boolean hasPermissionTo(String p) {
    return permissions.contains(p) || permissions.contains("*");
  }
}
