package ru.hh.nab;

import ru.hh.jdebug.jersey1.Jersey1DebugRecorder;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.JAXBException;

import static ru.hh.nab.Settings.getBoolProperty;

@Singleton
public class DebugInitializer {

  @Inject
  public DebugInitializer(Settings settings, AppMetadata metadata) throws JAXBException {
    if (isDebugEnabled(settings)) {
      initializeJerseyDebug(metadata);
    }
  }

  private void initializeJerseyDebug(AppMetadata metadata) throws JAXBException {
    Jersey1DebugRecorder.init(metadata.getName(), metadata.getVersion(), false, null);
  }

  private boolean isDebugEnabled(Settings settings) {
    return getBoolProperty(settings.subTree("jdebug"), "enabled", false);
  }
}
