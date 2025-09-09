package ru.hh.nab.telemetry;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import ru.hh.jclient.common.util.storage.Storage;
import ru.hh.jclient.common.util.storage.Transfer;

public class ContextStorage implements Storage<Context> {
  @Override
  public Context get() {
    return Context.current();
  }

  @Override
  public void set(Context context) { }

  @Override
  public void clear() { }

  @Override
  public Transfer prepareTransferToAnotherThread() {
    return new PreparedContextTransfer();
  }

  public static class PreparedContextTransfer implements Transfer {
    private final Context context = Context.current();
    private Scope scope;

    @Override
    public void perform() {
      scope = context.makeCurrent();
    }

    @Override
    public void rollback() {
      scope.close();
    }
  }
}
