package ru.hh.nab.datasource.monitoring.stack;

final class CompressedStackFactoryDefaults {
  static final String INNER_CLASS_EXCLUDING = "ru.hh.nab.datasource.monitoring.MonitoringConnection";
  static final String INNER_METHOD_EXCLUDING = "close";
  static final String OUTER_CLASS_EXCLUDING = "org.glassfish.jersey.servlet.ServletContainer";
  static final String OUTER_METHOD_EXCLUDING = "service";
  static final String[] INCLUDE_PACKAGES = new String[]{"ru.hh."};
  static final String[] EXCLUDE_CLASSES_PARTS = new String[]{"DataSourceContext", "ExecuteOnDataSource", "TransactionManager"};

  private CompressedStackFactoryDefaults() {
  }
}
