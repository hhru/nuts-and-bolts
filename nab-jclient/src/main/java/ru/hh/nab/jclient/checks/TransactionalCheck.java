package ru.hh.nab.jclient.checks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.HttpClientEventListener;

public class TransactionalCheck implements HttpClientEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalCheck.class);

  public enum Action {
    DO_NOTHING, LOG, RAISE
  }

  private Action action = Action.LOG;

  @Override
  public void beforeExecute(HttpClient httpClient) {
    if (action == Action.DO_NOTHING || !TransactionSynchronizationManager.isActualTransactionActive()) {
      return;
    }
    if (action == Action.RAISE) {
      throw new TransactionalCheckException();
    }
    if (action == Action.LOG) {
      LOGGER.warn("logging executeRequest in transaction", new TransactionalCheckException());
    }
  }

  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
  }

  public static class TransactionalCheckException extends RuntimeException {
    TransactionalCheckException() {
      super("transaction is active during executeRequest");
    }
  }
}
