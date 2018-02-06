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
    bind(Properties.class).annotatedWith(Names.named("settings.properties")).toInstance(settingsProperties);
    bind(Settings.class).toInstance(new Settings(settingsProperties));
  }

  @Named("limits-with-names")
  @Provides
  @Singleton
  List<LimitWithName> limitsWithName(LeakDetector detector) throws IOException {
    Properties props = new Properties();
    File file = new File(settingsDir, "limits.properties");
    if (file.isFile()) {
      try (final FileReader fileReader = new FileReader(file)) {
        props.load(fileReader);
      }
    }
    List<LimitWithName> ret = new ArrayList<>();

    for (String name : props.stringPropertyNames()) {
      String property = props.getProperty(name);
      int pos = property.indexOf(',');
      int max = Integer.parseInt(pos == -1 ? property : property.substring(0, pos));
      int warnThreshold = pos == -1 ? 0 : Integer.parseInt(property.substring(pos+1));

      Limit limit = new SimpleLimit(max, detector, name, warnThreshold);
      ret.add(new LimitWithName(limit, name));
    }
    return ret;
  }

  public static class LimitWithName {
    public final Limit limit;
    public final String name;

    public LimitWithName(Limit limit, String name) {
      this.limit = limit;
      this.name = name;
    }
  }
}
