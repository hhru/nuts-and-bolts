package ru.hh.nab.health.monitoring;

import org.slf4j.MDC;

public final class LoggingContext {
  private final static String REQ_H_X_REQUEST_ID = "req.h.x-request-id";
  private String backup;
  private String requestId;
  private Boolean replaced;

  private LoggingContext(String backup, String requestId, Boolean replaced) {
    this.backup = backup;
    this.requestId = requestId;
    this.replaced = replaced;
  }

  public static LoggingContext enter(String requestId) {
    LoggingContext lc = new LoggingContext(null, requestId, null);
    lc.enter();
    return lc;
  }

  public void enter() {
    if (replaced != null)
      throw new IllegalStateException("do not call this method twice or after leave");
    String old = MDC.get(REQ_H_X_REQUEST_ID);
    if (old == requestId || (old != null && old.equals(requestId))) {
      replaced = Boolean.FALSE;
    } else {
      if (requestId != null)
        MDC.put(REQ_H_X_REQUEST_ID, requestId);
      else
        MDC.remove(REQ_H_X_REQUEST_ID);
      backup = old;
      replaced = Boolean.TRUE;
    }
  }

  public static LoggingContext fromCurrentContext() {
    String requestId = MDC.get(REQ_H_X_REQUEST_ID);
    return new LoggingContext(null, requestId, null);
  }

  public void leave() {
    if (replaced == null)
      throw new IllegalStateException("do not call this method twice or before enter");
    if (replaced) {
      if (backup != null)
        MDC.put(REQ_H_X_REQUEST_ID, backup);
      else
        MDC.remove(REQ_H_X_REQUEST_ID);
    }
    replaced = null;
  }
}
