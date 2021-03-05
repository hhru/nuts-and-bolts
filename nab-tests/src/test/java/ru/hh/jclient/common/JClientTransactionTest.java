package ru.hh.jclient.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.LongAdder;
import javax.inject.Inject;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.hh.jclient.common.util.storage.SingletonStorage;
import ru.hh.nab.hibernate.HibernateTestConfig;
import ru.hh.nab.hibernate.transaction.TransactionalScope;
import ru.hh.nab.jclient.NabJClientConfig;
import ru.hh.nab.jclient.checks.TransactionalCheck;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
  classes = {HibernateTestConfig.class, NabJClientConfig.class, JClientTransactionTest.TestConfig.class}
)
public class JClientTransactionTest {
  private static final TestRequestDebug DEBUG = new TestRequestDebug(true);
  private static final HttpClientContext HTTP_CLIENT_CONTEXT = new HttpClientContext(Collections.emptyMap(), Collections.emptyMap(),
    List.of(() -> DEBUG));

  @Inject
  private TransactionalScope transactionalScope;
  @Inject
  private List<HttpClientEventListener> eventListeners;
  @Inject
  private TransactionalCheck transactionalCheck;

  private AsyncHttpClient httpClient;
  private HttpClientFactory httpClientFactory;

  @BeforeEach
  public void beforeTest() {
    httpClient = mock(AsyncHttpClient.class);
    when(httpClient.getConfig()).thenReturn(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(0).build());
    when(httpClient.executeRequest(isA(Request.class), isA(HttpClientImpl.CompletionHandler.class)))
      .then(iom -> {
        HttpClientImpl.CompletionHandler handler = iom.getArgument(1);
        handler.onCompleted(mock(Response.class));
        return null;
      });

    httpClientFactory = new HttpClientFactory(
      httpClient, Set.of(), new SingletonStorage<>(() -> HTTP_CLIENT_CONTEXT), Runnable::run, new DefaultRequestStrategy(), eventListeners, null);
  }

  @Test
  public void testJClientRequestInReadScope() {
    transactionalCheck.setAction(TransactionalCheck.Action.RAISE);
    transactionalScope.read(() -> {
      try {
        return httpClientFactory.with(new RequestBuilder().setUrl("http://test").build()).expectNoContent().result().get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testJClientRequestDoNotRaiseExceptionInWriteScope() {
    transactionalCheck.setAction(TransactionalCheck.Action.LOG);
    transactionalScope.write(() -> {
      try {
        httpClientFactory.with(new RequestBuilder().setUrl("http://test").build()).expectNoContent().result().get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return null;
    });
  }

  @Test
  public void testJClientRequestRaiseActionInWriteScope() {
    transactionalCheck.setAction(TransactionalCheck.Action.RAISE);
    Exception raisedException = transactionalScope.write(() -> {
      try {
        httpClientFactory.with(new RequestBuilder().setUrl("http://test").build()).expectNoContent().result().get();
      } catch (Exception e) {
        return e;
      }
      return null;
    });
    assertNotNull(raisedException);
    assertTrue(raisedException instanceof TransactionalCheck.TransactionalCheckException);
    assertEquals("transaction is active during executeRequest", raisedException.getMessage());
  }

  @Test
  public void testLogSkipsDefaultPackages() throws Exception {
    transactionalCheck.setAction(TransactionalCheck.Action.LOG);
    transactionalScope.write(() -> {
      try {
        httpClientFactory.with(new RequestBuilder().setUrl("http://test").build()).expectNoContent().result().get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return null;
    });
    Map<String, LongAdder> callInTxStatistics = transactionalCheck.getCallInTxStatistics();
    callInTxStatistics.forEach((stack, counter) -> assertFalse(
      stack.lines().anyMatch(line -> TransactionalCheck.DEFAULT_PACKAGES_TO_SKIP.stream().anyMatch(line::contains))
    ));
  }

  @Configuration
  static class TestConfig {

    @Bean
    ScheduledExecutorService scheduledExecutorService() {
      return Executors.newScheduledThreadPool(1);
    }

    @Bean
    String serviceName() {
      return "test";
    }
  }
}
