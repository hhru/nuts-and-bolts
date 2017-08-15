package ru.hh.nab.hibernate;

import java.net.URL;
import java.util.List;
import java.util.Properties;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;

public class NaBPersistenceUnitInfo implements PersistenceUnitInfo {

  private final String persistenceUnitName;
  private final DataSource dataSource;
  private final List<String> managedClassNames;
  private final Properties properties;

  public NaBPersistenceUnitInfo(String persistenceUnitName, DataSource dataSource, List<String> managedClassNames, Properties properties) {
    this.persistenceUnitName = persistenceUnitName;
    this.dataSource = dataSource;
    this.managedClassNames = managedClassNames;
    this.properties = properties;
  }

  @Override
  public String getPersistenceUnitName() {
    return persistenceUnitName;
  }

  @Override
  public String getPersistenceProviderClassName() {
    return HibernatePersistenceProvider.class.getName();
  }

  @Override
  public PersistenceUnitTransactionType getTransactionType() {
    return PersistenceUnitTransactionType.RESOURCE_LOCAL;
  }

  @Override
  public DataSource getJtaDataSource() {
    return null;
  }

  @Override
  public DataSource getNonJtaDataSource() {
    return dataSource;
  }

  @Override
  public List<String> getMappingFileNames() {
    return null;
  }

  @Override
  public List<URL> getJarFileUrls() {
    return null;
  }

  @Override
  public URL getPersistenceUnitRootUrl() {
    return null;
  }

  @Override
  public List<String> getManagedClassNames() {
    return managedClassNames;
  }

  @Override
  public boolean excludeUnlistedClasses() {
    return false;
  }

  @Override
  public SharedCacheMode getSharedCacheMode() {
    return SharedCacheMode.NONE;
  }

  @Override
  public ValidationMode getValidationMode() {
    return ValidationMode.AUTO;
  }

  @Override
  public Properties getProperties() {
    return properties;
  }

  @Override
  public String getPersistenceXMLSchemaVersion() {
    return "2.1";
  }

  @Override
  public ClassLoader getClassLoader() {
    return null;
  }

  @Override
  public void addTransformer(ClassTransformer transformer) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public ClassLoader getNewTempClassLoader() {
    return null;
  }
}
