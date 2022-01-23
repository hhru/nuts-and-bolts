package ru.hh.nab.test.contract;

import java.util.Map;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.nab.test.contract.service.ExpectationsStorage;

public class ContractTestExtension implements BeforeAllCallback, BeforeEachCallback {

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    String serviceName = (String) SpringExtension.getApplicationContext(extensionContext)
        .getBean("serviceName");
    ExpectationsStorage.setServiceName(serviceName);
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    var contextSupplier = SpringExtension.getApplicationContext(extensionContext)
        .getBean(HttpClientContextThreadLocalSupplier.class);
    contextSupplier.addContext(Map.of(), Map.of());
  }
}
