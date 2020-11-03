package ru.hh.nab.testbase.listeners.dto;

import java.time.Instant;

public class TestExecResult {
  private final Instant startTime;
  private Instant endTime;
  private String className;
  private String methodName;
  private ResultStatus status;
  private String message;

  public TestExecResult(String className, String methodName) {
    startTime = Instant.now();
    this.className = className;
    this.methodName = methodName;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public void setEndTime(Instant endTime) {
    this.endTime = endTime;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public ResultStatus getStatus() {
    return status;
  }

  public void setStatus(ResultStatus status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
