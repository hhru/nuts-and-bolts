package ru.hh.jclient.common;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Transactional;
import ru.hh.jclient.common.util.storage.SingletonStorage;
import ru.hh.nab.hibernate.HibernateTestConfig;
import ru.hh.nab.jclient.NabJClientConfig;
import ru.hh.nab.jclient.checks.TransactionalCheck;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration(
  classes = {HibernateTestConfig.class, NabJClientConfig.class, JClientTransactionTest.DataSourceContextTestConfig.class}
)
public class JClientTransactionTest extends AbstractJUnit4SpringContextTests {
  private static final TestRequestDebug DEBUG = new TestRequestDebug(true);
  private static final HttpClientContext HTTP_CLIENT_CONTEXT = new HttpClientContext(Collections.emptyMap(), Collections.emptyMap(), () -> DEBUG);

  @Inject
  private TransactionalScope transactionalScope;
  @Inject
  private HttpClientFactoryBuilder httpClientFactoryBuilder;
  @Inject
  private TransactionalCheck transactionalCheck;

  private AsyncHttpClient httpClient;
  private HttpClientFactory httpClientFactory;

  @Before
  public void beforeTest() {
    httpClient = mock(AsyncHttpClient.class);

    when(httpClient.executeRequest(isA(Request.class), isA(HttpClientImpl.CompletionHandler.class)))
      .then(iom -> {
        HttpClientImpl.CompletionHandler handler = iom.getArgument(1);
        handler.onCompleted(mock(Response.class));
        return null;
      });

    httpClientFactory = httpClientFactoryBuilder
      .withHostsWithSession(Set.of())
      .withStorage(new SingletonStorage<>(() -> HTTP_CLIENT_CONTEXT))
      .withCallbackExecutor(Runnable::run)
      .build();
  }

  @Test
  public void testJClientRequestInReadScope() throws Exception {
    transactionalScope.read(() -> {
      httpClientFactory.with(new RequestBuilder().setUrl("http://test").build()).expectEmpty().result().get();
      return null;
    });
  }

  @Test
  public void testJClientRequestInWriteScope() throws Exception {
    transactionalScope.write(() -> {
      httpClientFactory.with(new RequestBuilder().setUrl("http://test").build()).expectEmpty().result().get();
      return null;
    });
  }

  @Test
  public void testJClientRequestRaiseActionInWriteScope() throws Exception {
    transactionalCheck.setAction(TransactionalCheck.Action.RAISE);

    transactionalScope.write(() -> {
      boolean exceptionRaised = false;

      try {
        httpClientFactory.with(new RequestBuilder().setUrl("http://test").build()).expectEmpty().result().get();
      } catch (Exception e) {
        exceptionRaised = true;
        assertEquals("transaction is active during executeRequest", e.getMessage());
      }

      assertTrue(exceptionRaised);
      return null;
    });
  }

  @FunctionalInterface
  interface TargetMethod<T> {
    T invoke() throws Exception;
  }

  static class TransactionalScope {
    @Transactional(readOnly = true)
    public <T> T read(TargetMethod<T> method) throws Exception {
      return method.invoke();
    }

    @Transactional
    public <T> T write(TargetMethod<T> method) throws Exception {
      return method.invoke();
    }
  }

  @Configuration
  static class DataSourceContextTestConfig {
    @Bean
    TransactionalScope transactionalScope() {
      return new TransactionalScope();
    }

    @Bean
    String serviceName() {
      return "test";
    }
  }
}
