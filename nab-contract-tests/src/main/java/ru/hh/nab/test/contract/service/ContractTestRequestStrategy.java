package ru.hh.nab.test.contract.service;

import java.util.function.UnaryOperator;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.RequestStrategy;

public class ContractTestRequestStrategy implements RequestStrategy<ContractTestRequestEngineBuilder> {

  public ContractTestRequestStrategy(UnaryOperator<ContractTestRequestEngineBuilder> configAction) {
    this.configAction = configAction;
  }

  public ContractTestRequestStrategy() {
    this(UnaryOperator.identity());
  }

  private final UnaryOperator<ContractTestRequestEngineBuilder> configAction;

  @Override
  public ContractTestRequestEngineBuilder createRequestEngineBuilder(HttpClient client) {
    return new ContractTestRequestEngineBuilder();
  }

  @Override
  public RequestStrategy<ContractTestRequestEngineBuilder> createCustomizedCopy(UnaryOperator<ContractTestRequestEngineBuilder> configAction) {
    return new ContractTestRequestStrategy(this.configAction.andThen(configAction)::apply);
  }
}
