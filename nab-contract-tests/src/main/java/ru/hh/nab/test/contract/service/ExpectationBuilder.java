package ru.hh.nab.test.contract.service;

import java.util.concurrent.Callable;
import ru.hh.nab.test.contract.model.ExpectedResponse;

/**
 * Contract builder
 * Typical usage is:
 *
 *     expect(expectedResponse)
 *         .from(PROVIDER_NAME)
 *         .when(() -> jclient.someMethod(param1, param2));
 *
 */
public class ExpectationBuilder<T> {

  private ExpectationBuilder() {
  }

  private ExpectedResponse expectedResponse;
  private String providerName;

  public void when(Callable<T> callable) {
    validate();
    ExpectationsStorage.setPendingResponse(expectedResponse);
    ExpectationsStorage.setPendingProviderName(providerName);

    try {
      callable.call();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ExpectationBuilder<T> from(String providerName) {
    this.providerName = providerName;
    return this;
  }

  public static <T> ExpectationBuilder<T> expect(ExpectedResponse expectedResponse) {
    ExpectationBuilder<T> expectationBuilder = new ExpectationBuilder<>();
    expectationBuilder.expectedResponse = expectedResponse;
    return expectationBuilder;
  }

  private void validate() {
    if (providerName == null) {
      throw new RuntimeException("Provider name is not present");
    }
    if (expectedResponse == null) {
      throw new RuntimeException("Expected response is not present");
    }
  }
}
