package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import ru.hh.nab.security.PermissionLoader;
import ru.hh.nab.security.PropertiesPermissionLoader;

public class SettingsModule extends AbstractModule {
  private final File settingsDir;

  public SettingsModule() {
    settingsDir = new File(System.getProperty("settingsDir"));
  }

  @Override
  protected void configure() {
  }

  @Provides
  @Singleton
  protected Settings settings() throws IOException {
    Properties props = new Properties();
    props.load(new FileReader(new File(settingsDir, "settings.properties")));
    return new Settings(props);
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
}
