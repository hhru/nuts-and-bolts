package ru.hh.nab.test.contract.model;

public class Expectation {

  private ExpectedRequest expectedRequest;

  private ExpectedResponse expectedResponse;

  private String providerName;

  public Expectation(ExpectedRequest expectedRequest, ExpectedResponse expectedResponse, String providerName) {
    this.expectedRequest = expectedRequest;
    this.expectedResponse = expectedResponse;
    this.providerName = providerName;
  }

  public ExpectedRequest getExpectedRequest() {
    return expectedRequest;
  }

  public void setExpectedRequest(ExpectedRequest expectedRequest) {
    this.expectedRequest = expectedRequest;
  }

  public ExpectedResponse getExpectedResponse() {
    return expectedResponse;
  }

  public void setExpectedResponse(ExpectedResponse expectedResponse) {
    this.expectedResponse = expectedResponse;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }
}
