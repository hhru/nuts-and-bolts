package ru.hh.nab.datasource.monitoring.stack;

import static ru.hh.nab.datasource.monitoring.stack.CompressedStackFactoryDefaults.EXCLUDE_CLASSES_PARTS;
import static ru.hh.nab.datasource.monitoring.stack.CompressedStackFactoryDefaults.INCLUDE_PACKAGES;
import static ru.hh.nab.datasource.monitoring.stack.CompressedStackFactoryDefaults.INNER_CLASS_EXCLUDING;
import static ru.hh.nab.datasource.monitoring.stack.CompressedStackFactoryDefaults.INNER_METHOD_EXCLUDING;
import static ru.hh.nab.datasource.monitoring.stack.CompressedStackFactoryDefaults.OUTER_CLASS_EXCLUDING;
import static ru.hh.nab.datasource.monitoring.stack.CompressedStackFactoryDefaults.OUTER_METHOD_EXCLUDING;

public class CompressedStackFactoryConfig {

  private final String innerClassExcluding;
  private final String innerMethodExcluding;
  private final String outerClassExcluding;
  private final String outerMethodExcluding;
  private final String[] includePackages;
  private final String[] excludeClassesParts;

  public CompressedStackFactoryConfig() {
    this(new Builder());
  }

  public CompressedStackFactoryConfig(Builder builder) {
    this.innerClassExcluding = builder.innerClassExcluding;
    this.innerMethodExcluding = builder.innerMethodExcluding;
    this.outerClassExcluding = builder.outerClassExcluding;
    this.outerMethodExcluding = builder.outerMethodExcluding;
    this.includePackages = builder.includePackages;
    this.excludeClassesParts = builder.excludeClassesParts;
  }

  public String getInnerClassExcluding() {
    return innerClassExcluding;
  }

  public String getInnerMethodExcluding() {
    return innerMethodExcluding;
  }

  public String getOuterClassExcluding() {
    return outerClassExcluding;
  }

  public String getOuterMethodExcluding() {
    return outerMethodExcluding;
  }

  public String[] getIncludePackages() {
    return includePackages;
  }

  public String[] getExcludeClassesParts() {
    return excludeClassesParts;
  }

  public static class Builder {
    private String innerClassExcluding = INNER_CLASS_EXCLUDING;
    private String innerMethodExcluding = INNER_METHOD_EXCLUDING;
    private String outerClassExcluding = OUTER_CLASS_EXCLUDING;
    private String outerMethodExcluding = OUTER_METHOD_EXCLUDING;
    private String[] includePackages = INCLUDE_PACKAGES;
    private String[] excludeClassesParts = EXCLUDE_CLASSES_PARTS;

    public Builder() {
    }

    public Builder withInnerClassExcluding(String innerClassExcluding) {
      this.innerClassExcluding = innerClassExcluding;
      return this;
    }

    public Builder withInnerMethodExcluding(String innerMethodExcluding) {
      this.innerMethodExcluding = innerMethodExcluding;
      return this;
    }

    public Builder withOuterClassExcluding(String outerClassExcluding) {
      this.outerClassExcluding = outerClassExcluding;
      return this;
    }

    public Builder withOuterMethodExcluding(String outerMethodExcluding) {
      this.outerMethodExcluding = outerMethodExcluding;
      return this;
    }

    public Builder withIncludePackages(String[] includePackages) {
      this.includePackages = includePackages;
      return this;
    }

    public Builder withExcludeClassesParts(String[] excludeClassesParts) {
      this.excludeClassesParts = excludeClassesParts;
      return this;
    }

    public CompressedStackFactoryConfig build() {
      return new CompressedStackFactoryConfig(this);
    }
  }
}
