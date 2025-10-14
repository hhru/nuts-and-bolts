package ru.hh.nab.jclient.checks;

import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.concurrent.TimeUnit.MINUTES;
import java.util.concurrent.atomic.LongAdder;
import javax.sql.DataSource;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.hh.jclient.common.DefaultRequestStrategy;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.util.storage.SingletonStorage;
import ru.hh.nab.common.executor.ScheduledExecutor;
import ru.hh.trace.TraceContext;

@SpringBootTest(classes = TransactionalCheckTest.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class TransactionalCheckTest {

  private static final TransactionalCheck transactionalCheck = new TransactionalCheck(
      null,
      10,
      new ScheduledExecutor(),
      MINUTES.toMillis(1),
      Set.of()
  );
  private static final HttpClientContext HTTP_CLIENT_CONTEXT = new HttpClientContext(
      Collections.emptyMap(),
      Collections.emptyMap(),
      List.of(() -> transactionalCheck)
  );

  private static Runnable jClientRequest;

  @Inject
  private TransactionalScope transactionalScope;

  @BeforeAll
  public static void beforeTests() {
    AsyncHttpClient httpClient = mock(AsyncHttpClient.class);
    when(httpClient.getConfig()).thenReturn(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(0).build());

    HttpClientFactory httpClientFactory = new HttpClientFactory(
        httpClient,
        new SingletonStorage<>(HTTP_CLIENT_CONTEXT),
        Set.of(),
        Runnable::run,
        new DefaultRequestStrategy(),
        mock(TraceContext.class)
    );

    jClientRequest = () -> httpClientFactory.with(new RequestBuilder().setUrl("http://test").build()).expectNoContent().result();
  }

  @Test
  public void testJClientRequestDoNotRaiseExceptionIfTransactionIsNonActive() {
    transactionalCheck.setAction(TransactionalCheck.Action.RAISE);
    assertDoesNotThrow(() -> transactionalScope.read(jClientRequest));
  }

  @Test
  public void testJClientRequestDoNotRaiseExceptionIfActionIsDoNothing() {
    transactionalCheck.setAction(TransactionalCheck.Action.DO_NOTHING);
    assertDoesNotThrow(() -> transactionalScope.write(jClientRequest));
  }

  @Test
  public void testJClientRequestDoNotRaiseExceptionIfActionIsLog() {
    transactionalCheck.setAction(TransactionalCheck.Action.LOG);
    assertDoesNotThrow(() -> transactionalScope.write(jClientRequest));
  }

  @Test
  public void testJClientRequestRaiseExceptionIfTransactionIsActive() {
    transactionalCheck.setAction(TransactionalCheck.Action.RAISE);
    TransactionalCheck.TransactionalCheckException raisedException = assertThrows(
        TransactionalCheck.TransactionalCheckException.class,
        () -> transactionalScope.write(jClientRequest)
    );
    assertNotNull(raisedException);
    assertEquals("transaction is active during executeRequest", raisedException.getMessage());
  }

  @Test
  public void testLogSkipsDefaultPackages() {
    transactionalCheck.setAction(TransactionalCheck.Action.LOG);
    transactionalScope.write(jClientRequest);
    Map<String, LongAdder> callInTxStatistics = transactionalCheck.getCallInTxStatistics();
    callInTxStatistics.forEach((stack, counter) -> assertFalse(
        stack.lines().anyMatch(line -> TransactionalCheck.DEFAULT_PACKAGES_TO_SKIP.stream().anyMatch(line::contains))
    ));
  }

  @Configuration
  @EnableTransactionManagement
  @Import(TransactionalScope.class)
  public static class TestConfiguration {

    @Bean
    public TransactionManager transactionManager() throws SQLException {
      DataSource dataSource = mock(DataSource.class);
      when(dataSource.getConnection()).thenReturn(mock(Connection.class));
      return new DataSourceTransactionManager(dataSource);
    }
  }

  static class TransactionalScope {

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    void read(Runnable runnable) {
      runnable.run();
    }

    @Transactional
    void write(Runnable runnable) {
      runnable.run();
    }
  }
}
