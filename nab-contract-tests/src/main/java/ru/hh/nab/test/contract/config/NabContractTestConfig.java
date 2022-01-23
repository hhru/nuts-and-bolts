package ru.hh.nab.test.contract.config;

import java.util.concurrent.Executor;
import javax.inject.Named;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.HttpClientFactoryBuilder;
import ru.hh.nab.common.executor.MonitoredThreadPoolExecutor;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.jclient.NabJClientConfig;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.test.contract.service.ContractTestRequestStrategy;

@Configuration
@Import({NabJClientConfig.class})
public class NabContractTestConfig {

  public static final String CONTRACT_TEST_HTTP_CLIENT_FACTORY = "contractTestHttpClientFactory";

  @Bean
  @Named(CONTRACT_TEST_HTTP_CLIENT_FACTORY)
  HttpClientFactory contractTestHttpClientFactory(HttpClientFactoryBuilder httpClientFactoryBuilder,
                                                  FileSettings fileSettings,
                                                  @Named(SERVICE_NAME) String serviceName,
                                                  StatsDSender statsDSender,
                                                  HttpClientContextThreadLocalSupplier contextStorage) {
    FileSettings jClientSettings = fileSettings.getSubSettings("jclient");
    Executor callbackExecutor = MonitoredThreadPoolExecutor.create(
        jClientSettings.getSubSettings("threadPool"), "jclient", statsDSender, serviceName
    );
    return httpClientFactoryBuilder
        .withRequestStrategy(new ContractTestRequestStrategy())
        .withCallbackExecutor(callbackExecutor)
        .withStorage(contextStorage)
        .build();
  }
}
