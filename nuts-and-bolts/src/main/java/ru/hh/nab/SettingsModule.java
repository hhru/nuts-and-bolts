package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class SettingsModule extends AbstractModule {
  @Override
  protected void configure() {
  }

  @Provides
  @Singleton
  protected Settings settings() throws IOException {
    Properties props = new Properties();
    props.load(new FileReader(new File(System.getProperty("settingsDir"), "settings.properties").getAbsolutePath()));
    return new Settings(props);
  }
}
