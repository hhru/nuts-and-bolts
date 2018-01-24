package ru.hh.nab.hibernate;

import java.util.Arrays;
import java.util.HashSet;

public class MappingConfig {
  private final HashSet<Class<?>> mappings = new HashSet<>();

  public MappingConfig(Class<?> ...classes) {
    mappings.addAll(Arrays.asList(classes));
  }

  public void addMapping(Class<?> entityClass) {
    mappings.add(entityClass);
  }

  public Class<?>[] getMappings() {
    return mappings.toArray(new Class<?>[mappings.size()]);
  }
}
