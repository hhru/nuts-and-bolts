package ru.hh.nab.jclient.storage;

import ru.hh.jclient.common.util.storage.Storage;
import ru.hh.jclient.common.util.storage.Transfer;
import ru.hh.trace.Scope;
import ru.hh.trace.TraceContext;
import ru.hh.trace.TraceContextTransfer;

public class TraceContextStorage implements Storage<TraceContext> {

  private final TraceContext traceContext;

  public TraceContextStorage(TraceContext traceContext) {
    this.traceContext = traceContext;
  }

  @Override
  public TraceContext get() {
    return traceContext;
  }

  @Override
  public void set(TraceContext traceContext) {
  }

  @Override
  public void clear() {
  }

  @Override
  public Transfer prepareTransferToAnotherThread() {
    return new PreparedTraceContextTransfer();
  }

  private class PreparedTraceContextTransfer implements Transfer {

    private final TraceContextTransfer traceContextTransfer = traceContext.getTransfer();
    private Scope scope;

    @Override
    public void perform() {
      scope = traceContext.restore(traceContextTransfer);
    }

    @Override
    public void rollback() {
      scope.close();
    }
  }
}
