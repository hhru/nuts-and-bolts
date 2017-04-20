package ru.hh.nab;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Properties;
import javax.inject.Named;
import javax.inject.Singleton;
import com.google.inject.name.Names;
import ru.hh.nab.health.limits.LeakDetector;
import ru.hh.nab.health.limits.Limit;
import ru.hh.nab.health.limits.SimpleLimit;
import ru.hh.nab.health.monitoring.Dumpable;
import ru.hh.nab.security.PermissionLoader;
import ru.hh.nab.security.PropertiesPermissionLoader;

public class SettingsModule extends AbstractModule {
  private final File settingsDir;

  public SettingsModule() {
    settingsDir = new File(System.getProperty("settingsDir"));
  }

  @Override
  protected void configure() {
    final Properties props = loadProperties(settingsDir);
    Names.bindProperties(binder(), props);
    bind(Settings.class).toProvider(() -> new Settings(props)).asEagerSingleton();
  }

  static Properties loadProperties(File settingsDir) {
    final Properties defaultProps = new Properties();
    final Properties props = new Properties(defaultProps);
    try {
      fillProperties(defaultProps, new File(settingsDir, "settings.properties"));
      final File devSettingsFile = new File(settingsDir, "settings.properties.dev");
      if (devSettingsFile.exists()) {
        fillProperties(props, devSettingsFile);
      }
      return props;
    } catch (IOException e) {
      throw new IllegalStateException("Error reading settings", e);
    }
  }

  private static void fillProperties(Properties properties, File file) throws IOException {
    try (Reader propertiesReader = new FileReader(file)) {
      properties.load(propertiesReader);
    }
  }

  @Provides
  @Singleton
  protected PermissionLoader permissionLoader() throws IOException {
    Properties props = new Properties();
    File file = new File(settingsDir, "api-security.properties");
    if (file.isFile()) {
      props.load(new FileReader(file));
    }
    return new PropertiesPermissionLoader(props);
  }

  @Named("limits-with-names")
  @Provides
  @Singleton
  List<LimitWithNameAndHisto> limitsWithNameAndHisto(LeakDetector detector) throws IOException {
    Properties props = new Properties();
    File file = new File(settingsDir, "limits.properties");
    if (file.isFile()) {
      props.load(new FileReader(file));
    }
    List<LimitWithNameAndHisto> ret = Lists.newArrayList();

    for (String name : props.stringPropertyNames()) {
      String property = props.getProperty(name);
      int pos = property.indexOf(',');
      int max = Integer.parseInt(pos == -1 ? property : property.substring(0, pos));
      int warnThreshold = pos == -1 ? 0 : Integer.parseInt(property.substring(pos+1));
// CountingHistogramImpl<Integer> histo = new CountingHistogramImpl<Integer>(Mappers.eqMapper(max));

      Limit limit = new SimpleLimit(max, detector, name, warnThreshold);
      ret.add(new LimitWithNameAndHisto(limit, name, null));
// new CountingHistogramQuantilesDumpable<Integer>(histo, 0.5, 0.75, 0.9, 0.95, 0.99, 1.0)));
    }
    return ret;
  }

  public static class LimitWithNameAndHisto {
    public final Limit limit;
    public final String name;
    public final Dumpable histo;

    public LimitWithNameAndHisto(Limit limit, String name, Dumpable histo) {
      this.limit = limit;
      this.name = name;
      this.histo = histo;
    }
  }
}
