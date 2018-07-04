package ru.hh.nab.hibernate;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class MappingConfig {
  private final Set<Class<?>> annotatedClasses = new LinkedHashSet<>();
  private final Set<String> packagesToScan = new LinkedHashSet<>();

  public MappingConfig(Class<?>... entityClasses) {
    annotatedClasses.addAll(Arrays.asList(entityClasses));
  }

  public void addEntityClass(Class<?> entityClass) {
    annotatedClasses.add(entityClass);
  }

  public void addPackagesToScan(String... packageNames) {
    packagesToScan.addAll(Arrays.asList(packageNames));
  }

  public Class<?>[] getAnnotatedClasses() {
    return annotatedClasses.toArray(new Class<?>[0]);
  }

  public String[] getPackagesToScan() {
    return packagesToScan.toArray(new String[0]);
  }
}
