package ru.hh.nab.test.contract.service;

import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestEngine;
import ru.hh.jclient.common.RequestEngineBuilder;
import ru.hh.jclient.common.RequestStrategy;

public class ContractTestRequestEngineBuilder implements RequestEngineBuilder<ContractTestRequestEngineBuilder>  {

  @Override
  public RequestEngine build(Request request, RequestStrategy.RequestExecutor requestExecutor) {
    return new ContractTestRequestEngine(request);
  }

  @Override
  public ContractTestRequestEngineBuilder withTimeoutMultiplier(Double timeoutMultiplier) {
    return this;
  }

  @Override
  public HttpClient backToClient() {
    throw new IllegalStateException("Not supported in contract test request engine");
  }
}
