package ru.hh.nab.hibernate;

import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXBException;
import ru.hh.jdebug.jdbc.JdbcDebugSupplier;

public class DebugInitializer {

  @Inject
  public DebugInitializer(@Named("settings.properties") Properties settingsProperties) throws JAXBException {
    String logFullQuery = HibernateModule.subTree("jdebug", settingsProperties).getProperty("logFullQuery", "false");
    JdbcDebugSupplier.setFullQueryLogging(Boolean.valueOf(logFullQuery));
  }

}
