package ru.hh.nab.test.contract.model;

import java.util.List;

public class Expectations {

  private String serviceName;
  private List<Expectation> expectations;

  public Expectations() {
  }

  public Expectations(String serviceName, List<Expectation> expectations) {
    this.serviceName = serviceName;
    this.expectations = expectations;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public List<Expectation> getExpectations() {
    return expectations;
  }

  public void setExpectations(List<Expectation> expectations) {
    this.expectations = expectations;
  }
}
