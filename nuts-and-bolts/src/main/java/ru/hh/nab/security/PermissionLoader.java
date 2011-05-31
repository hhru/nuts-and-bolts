package ru.hh.nab.security;

import com.google.inject.ImplementedBy;

@ImplementedBy(NullPermissionLoader.class)
public interface PermissionLoader {
  Permissions forKey(String key);
  Permissions anonymous();
}
