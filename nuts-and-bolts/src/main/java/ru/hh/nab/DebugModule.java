package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.jdebug.jclient.RequestDebugSupplier;
import ru.hh.jdebug.jersey1.provider.DebugListener;
import ru.hh.jdebug.jersey1.provider.jclient.HttpClientContextListener;

import javax.inject.Singleton;

public class DebugModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DebugInitializer.class).asEagerSingleton();
  }

  @Provides
  @Singleton
  protected DebugListener getDebugListener() {
    return new DebugListener();
  }

  @Provides
  @Singleton
  protected HttpClientContextListener getHttpClientContextListener(HttpClientContextThreadLocalSupplier supplier) {
    return new HttpClientContextListener(supplier);
  }

  @Provides
  @Singleton
  protected HttpClientContextThreadLocalSupplier getHttpClientContextSupplier() {
    return new HttpClientContextThreadLocalSupplier(new RequestDebugSupplier());
  }
}

