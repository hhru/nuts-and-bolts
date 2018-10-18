package ru.hh.nab.starter.servlet;

import java.util.Arrays;
import java.util.List;
import org.glassfish.jersey.server.ResourceConfig;

public class NabJerseyConfig {

  private static final String[] DEFAULT_MAPPING = {"/*"};

  public static final NabJerseyConfig NONE = new NabJerseyConfig(true);

  private final boolean disabled;

  public NabJerseyConfig() {
    this.disabled = false;
  }

  private NabJerseyConfig(boolean disabled) {
    this.disabled = disabled;
  }

  public static NabJerseyConfig forResources(Class<?>... resources) {
    return new NabJerseyConfig() {
      @Override
      public void configure(ResourceConfig resourceConfig) {
        Arrays.stream(resources).forEach(resourceConfig::register);
      }
    };
  }

  public String[] getMapping() {
    return DEFAULT_MAPPING;
  }

  public String getName() {
    return "jersey";
  }

  public List<String> getAllowedPackages() {
    return Arrays.asList("ru.hh", "com.headhunter");
  }

  public void configure(ResourceConfig resourceConfig) {

  }

  public final boolean isDisabled() {
    return disabled;
  }
}
