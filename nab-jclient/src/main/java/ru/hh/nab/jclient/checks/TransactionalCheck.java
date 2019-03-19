package ru.hh.nab.jclient.checks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.HttpClientEventListener;

import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;

public class TransactionalCheck implements HttpClientEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalCheck.class);

  public enum Action {
    DO_NOTHING, LOG, RAISE
  }

  private Action action = Action.LOG;

  @Override
  public void beforeExecute(HttpClient httpClient) {
    if (action != Action.DO_NOTHING) {
      if (isActualTransactionActive()) {
        try {
          throw new TransactionalCheckException();
        } catch (RuntimeException e) {
          if (action == Action.LOG) {
            LOGGER.warn("logging executeRequest in transaction", e);
          } else if (action == Action.RAISE) {
            throw e;
          }
        }
      }
    }
  }

  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
  }

  class TransactionalCheckException extends RuntimeException {
    TransactionalCheckException() {
      super("transaction is active during executeRequest");
    }
  }
}
