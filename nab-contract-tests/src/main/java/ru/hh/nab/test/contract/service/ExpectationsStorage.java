package ru.hh.nab.test.contract.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ru.hh.jclient.common.Param;
import ru.hh.jclient.common.Request;
import ru.hh.nab.test.contract.model.Expectation;
import ru.hh.nab.test.contract.model.Expectations;
import ru.hh.nab.test.contract.model.ExpectedRequest;
import ru.hh.nab.test.contract.model.ExpectedResponse;

public class ExpectationsStorage {

  private final static List<Expectation> expectations = new ArrayList<>();
  private final static ThreadLocal<ExpectedResponse> pendingResponse = new ThreadLocal<>();
  private final static ThreadLocal<String> pendingProviderName = new ThreadLocal<>();

  private static String serviceName;

  public static void setPendingResponse(ExpectedResponse expectedResponse) {
    pendingResponse.set(expectedResponse);
  }

  public static void setPendingProviderName(String providerName) {
    pendingProviderName.set(providerName);
  }

  public static void setServiceName(String serviceName) {
    ExpectationsStorage.serviceName = serviceName;
  }

  public static ExpectedResponse add(Request request) {
    ExpectedResponse expectedResponse = pendingResponse.get();
    String providerName = pendingProviderName.get();
    if (expectedResponse == null) {
      throw new RuntimeException("No pending response");
    }

    if (providerName == null) {
      throw new RuntimeException("No pending provider name");
    }

    ExpectedRequest expectedRequest = mapToExpectedRequest(request);

    expectations.add(new Expectation(expectedRequest, expectedResponse, providerName));
    ExpectationsStorage.pendingResponse.remove();
    ExpectationsStorage.pendingProviderName.remove();

    return expectedResponse;
  }

  private static ExpectedRequest mapToExpectedRequest(Request request) {
    ExpectedRequest expectedRequest = new ExpectedRequest();
    expectedRequest.setBody(request.getStringData());
    Map<String, List<String>> headers = request.getHeaders()
        .stream()
        .filter(entry -> entry.getValue() != null)
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> List.of(entry.getValue())));
    expectedRequest.setHeaders(headers);

    expectedRequest.setMethod(request.getMethod());
    expectedRequest.setPath(request.getUri().getPath());

    Map<String, List<String>> queryParams = request.getQueryParams()
        .stream()
        .filter(entry -> entry.getValue() != null)
        .collect(Collectors.toMap(Param::getName, param -> List.of(param.getValue())));

    expectedRequest.setQueryParams(queryParams);
    return expectedRequest;
  }

  public static Expectations getExpectations() {
    return new Expectations(serviceName, expectations);
  }
}
