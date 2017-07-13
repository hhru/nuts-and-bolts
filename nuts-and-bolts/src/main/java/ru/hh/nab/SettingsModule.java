package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import javax.inject.Named;
import javax.inject.Singleton;
import com.google.inject.name.Names;
import java.util.ArrayList;
import ru.hh.nab.health.limits.LeakDetector;
import ru.hh.nab.health.limits.Limit;
import ru.hh.nab.health.limits.SimpleLimit;
import ru.hh.nab.health.monitoring.Dumpable;
import ru.hh.nab.security.PermissionLoader;
import ru.hh.nab.security.PropertiesPermissionLoader;

public class SettingsModule extends AbstractModule {
  private final File settingsDir;
  private final Properties settingsProperties;

  public SettingsModule() {
    settingsDir = new File(System.getProperty("settingsDir"));

    settingsProperties = new Properties();
    try (final FileReader fileReader = new FileReader(new File(settingsDir, "settings.properties"))) {
      settingsProperties.load(fileReader);
    } catch (IOException e) {
      throw new IllegalStateException("Error reading settings.properties at " + settingsDir.getAbsolutePath(), e);
    }

    if (settingsProperties.getProperty("serviceName") == null) {
      throw new IllegalStateException("Property 'serviceName' not found in settings");
    }
  }

  @Override
  protected void configure() {
    Names.bindProperties(binder(), settingsProperties);
    bind(Settings.class).toInstance(new Settings(settingsProperties));
  }

  @Provides
  @Singleton
  protected PermissionLoader permissionLoader() throws IOException {
    Properties props = new Properties();
    File file = new File(settingsDir, "api-security.properties");
    if (file.isFile()) {
      try (final FileReader fileReader = new FileReader(file)) {
        props.load(fileReader);
      }
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
      try (final FileReader fileReader = new FileReader(file)) {
        props.load(fileReader);
      }
    }
    List<LimitWithNameAndHisto> ret = new ArrayList<>();

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
