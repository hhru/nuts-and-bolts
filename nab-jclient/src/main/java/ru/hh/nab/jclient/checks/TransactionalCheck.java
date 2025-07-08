package ru.hh.nab.jclient.checks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import static java.util.stream.Collectors.joining;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;

public class TransactionalCheck implements HttpClientEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalCheck.class);
  private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
  public static final Set<String> DEFAULT_PACKAGES_TO_SKIP = Set.of(
      TransactionalCheck.class.getPackageName(),
      "ru.hh.jclient.common",
      "org.springframework"
  );
  private final ConcurrentMap<String, LongAdder> callInTxStatistics;
  private final Set<String> packagesToSkip;
  private final int stackTraceDepthLimit;
  private final ScheduledFuture<?> publisher;
  private Action action;


  /**
   * @deprecated use {@link TransactionalCheck#TransactionalCheck(ru.hh.nab.jclient.checks.TransactionalCheck.Action,
   * int, java.util.concurrent.ScheduledExecutorService, long, java.lang.String...)}
   */
  @Deprecated(forRemoval = true)
  public TransactionalCheck() {
    action = Action.LOG;
    publisher = null;
    callInTxStatistics = null;
    packagesToSkip = null;
    stackTraceDepthLimit = -1;
  }

  public TransactionalCheck(
      Action action,
      int filteredTraceStackDepthLimit,
      ScheduledExecutorService executorService,
      long intervalMs,
      Set<String> packagesToSkip
  ) {
    this.action = action;
    this.stackTraceDepthLimit = filteredTraceStackDepthLimit;
    callInTxStatistics = new ConcurrentHashMap<>();
    this.packagesToSkip = new HashSet<>(DEFAULT_PACKAGES_TO_SKIP);
    this.packagesToSkip.addAll(packagesToSkip);
    publisher = executorService.scheduleAtFixedRate(() -> logCallsFromTx(intervalMs), 0, intervalMs, TimeUnit.MILLISECONDS);
  }

  private void logCallsFromTx(long intervalMs) {
    var copy = Map.copyOf(callInTxStatistics);
    callInTxStatistics.clear();
    copy.forEach((data, counter) -> {
      long currentRequestCount = counter.sum();
      LOGGER.error(
          "For last {} ms, got {} jclient calls inside tx:{}{}",
          intervalMs,
          currentRequestCount,
          System.lineSeparator(),
          data
      );
    });
  }

  @Override
  public void beforeExecute(HttpClient httpClient, RequestBuilder ignore, Request request) {
    if (action == Action.DO_NOTHING || !TransactionSynchronizationManager.isActualTransactionActive()) {
      return;
    }
    if (action == Action.RAISE) {
      throw new TransactionalCheckException();
    }
    if (action == Action.LOG) {
      if (publisher == null) {
        LOGGER.warn("logging executeRequest in transaction", new TransactionalCheckException());
      } else {
        var targetStackTrace = STACK_WALKER.walk(
            stackStream -> stackStream
                .filter(frame -> packagesToSkip
                    .stream()
                    .noneMatch(packageToSkip -> frame.getDeclaringClass().getPackageName().startsWith(packageToSkip))
                )
                .limit(stackTraceDepthLimit)
                .map(StackWalker.StackFrame::toStackTraceElement)
                .map(StackTraceElement::toString)
                .collect(joining(System.lineSeparator()))
        );
        callInTxStatistics.computeIfAbsent(targetStackTrace, data -> new LongAdder()).increment();
      }
    }
  }

  public Action getAction() {
    return action;
  }

  // Only for testing
  public void setAction(Action action) {
    this.action = action;
  }

  public Map<String, LongAdder> getCallInTxStatistics() {
    return Map.copyOf(callInTxStatistics);
  }

  public static class TransactionalCheckException extends RuntimeException {
    TransactionalCheckException() {
      super("transaction is active during executeRequest");
    }
  }

  public enum Action {
    DO_NOTHING, LOG, RAISE
  }
}
