package ru.hh.nab.hibernate.properties;

import java.util.Properties;
import org.hibernate.cfg.AvailableSettings;
import ru.hh.nab.hibernate.interceptor.ControllerPassingInterceptor;
import ru.hh.nab.hibernate.interceptor.RequestIdPassingInterceptor;
import ru.hh.nab.jpa.JpaPropertiesProvider;

public class HibernatePropertiesProvider implements JpaPropertiesProvider {

  private final Properties hibernateProperties;

  public HibernatePropertiesProvider(Properties hibernateProperties) {
    this.hibernateProperties = hibernateProperties;
    enrichProperties(this.hibernateProperties);
    validateProperties(this.hibernateProperties);
  }

  @Override
  public Properties get() {
    return hibernateProperties;
  }

  private void enrichProperties(Properties hibernateProperties) {
    // if set to true, it slows down acquiring database connection on application start
    hibernateProperties.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");

    // used to retrieve natively generated keys after insert
    // if set to false, Hibernate will retrieve key directly from sequence
    // and can fail if GenerationType = IDENTITY and sequence name is non-standard
    hibernateProperties.setProperty("hibernate.jdbc.use_get_generated_keys", "true");
  }

  private void validateProperties(Properties hibernateProperties) {
    String interceptorClassName = hibernateProperties.getProperty(AvailableSettings.STATEMENT_INSPECTOR);
    if (interceptorClassName == null) {
      return;
    }

    if (!interceptorClassName.equals(RequestIdPassingInterceptor.class.getCanonicalName())
        && !interceptorClassName.equals(ControllerPassingInterceptor.class.getCanonicalName())) {
      String msg = String.format("unknown value of '%s' property", AvailableSettings.STATEMENT_INSPECTOR);
      throw new RuntimeException(msg);
    }
  }
}
