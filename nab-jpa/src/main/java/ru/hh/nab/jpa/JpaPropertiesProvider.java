package ru.hh.nab.jpa;

import java.util.Properties;
import java.util.function.Supplier;

@FunctionalInterface
public interface JpaPropertiesProvider extends Supplier<Properties> {
}
