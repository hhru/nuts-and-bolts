package ru.hh.nab;

import ru.hh.jdebug.jersey1.Jersey1DebugRecorder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXBException;

import static ru.hh.nab.Settings.getBoolProperty;
import java.util.Properties;

public class DebugInitializer {

  @Inject
  public DebugInitializer(@Named("settings.properties") Properties settingsProperties, AppMetadata metadata) throws JAXBException {
    if (getBoolProperty(settingsProperties, "jdebug.enabled", false)) {
      Jersey1DebugRecorder.init(metadata.getName(), metadata.getVersion(), false, null);
    }
  }
}
