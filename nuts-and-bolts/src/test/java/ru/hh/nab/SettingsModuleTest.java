package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import ru.hh.nab.health.limits.LeakDetector;

public class SettingsModuleTest {

  @Test
  public void testConfigure() throws IOException {
    initFiles("port=1111", null);
    System.setProperty("settingsDir", ".");
    Module module = Modules.override(new SettingsModule()).with(new AbstractModule() {
      @Override
      protected void configure() {
        bind(LeakDetector.class).toInstance(mock(LeakDetector.class));
      }
    });
    Injector injector = Guice.createInjector(module);
    CheckInject check = injector.getInstance(CheckInject.class);
    assertEquals(1111, check.settings.port);
  }

  @Test
  public void testOverridenConfigure() throws IOException {
    initFiles("port=1111", "port=2222");
    System.setProperty("settingsDir", ".");
    Module module = Modules.override(new SettingsModule()).with(new AbstractModule() {
      @Override
      protected void configure() {
        bind(LeakDetector.class).toInstance(mock(LeakDetector.class));
      }
    });
    Injector injector = Guice.createInjector(module);
    CheckInject check = injector.getInstance(CheckInject.class);
    assertEquals(2222, check.settings.port);
  }

  private void initFiles(String defaultValue, String actualValue) throws IOException {
    File current = new File(".");
    if (defaultValue != null) {
      File defaultProperties = new File(current, "settings.properties");
      defaultProperties.createNewFile();
      FileUtils.write(defaultProperties, defaultValue, "UTF-8");
    }
    if (actualValue != null) {
      File properties = new File(current, "settings.properties.dev");
      properties.createNewFile();
      FileUtils.write(properties, actualValue, "UTF-8");
    }
  }

  @After
  public void removeSettings() {
    File current = new File(".");
    Stream.of(current.listFiles(file -> file.isFile() && file.getName().startsWith("settings.properties"))).forEach(File::delete);
  }

  static class CheckInject {
    @Inject
    Settings settings;
  }
}
