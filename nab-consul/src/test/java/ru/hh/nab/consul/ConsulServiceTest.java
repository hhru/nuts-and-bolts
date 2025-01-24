package ru.hh.nab.consul;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import ru.hh.consul.AgentClient;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.config.ClientConfig;
import ru.hh.consul.model.ConsulResponse;
import ru.hh.consul.model.agent.Registration;
import ru.hh.consul.model.catalog.ServiceWeights;
import ru.hh.consul.model.kv.Value;
import ru.hh.consul.monitoring.ClientEventCallback;
import ru.hh.consul.monitoring.ClientEventHandler;
import ru.hh.consul.option.QueryOptions;
import static ru.hh.nab.consul.ConsulService.CONSUL_CHECK_FAIL_COUNT_PROPERTY;
import static ru.hh.nab.consul.ConsulService.CONSUL_CHECK_HOST_PROPERTY;
import static ru.hh.nab.consul.ConsulService.CONSUL_CHECK_INTERVAL_PROPERTY;
import static ru.hh.nab.consul.ConsulService.CONSUL_CHECK_PASSING_PROPERTY;
import static ru.hh.nab.consul.ConsulService.CONSUL_CHECK_SUCCESS_COUNT_PROPERTY;
import static ru.hh.nab.consul.ConsulService.CONSUL_CHECK_TIMEOUT_PROPERTY;
import static ru.hh.nab.consul.ConsulService.CONSUL_DEREGISTER_CRITICAL_TIMEOUT_PROPERTY;
import static ru.hh.nab.consul.ConsulService.DEFAULT_CHECK_FAIL_COUNT;
import static ru.hh.nab.consul.ConsulService.DEFAULT_CHECK_HOST;
import static ru.hh.nab.consul.ConsulService.DEFAULT_CHECK_INTERVAL;
import static ru.hh.nab.consul.ConsulService.DEFAULT_CHECK_SUCCESS_COUNT;
import static ru.hh.nab.consul.ConsulService.DEFAULT_CHECK_TIMEOUT;
import static ru.hh.nab.consul.ConsulService.DEFAULT_DEREGISTER_CRITICAL_TIMEOUT;
import static ru.hh.nab.consul.ConsulService.META_SERVICE_VERSION_KEY;
import static ru.hh.nab.consul.ConsulService.PASSING_STATUS;

public class ConsulServiceTest {

  private static final String TEST_SERVICE_NAME = "testService";
  private static final String TEST_SERVICE_VERSION = "test-version";
  private static final String TEST_NODE_NAME = "testNode";
  private static final int TEST_APPLICATION_PORT = 8090;
  private static final int TEST_WEIGHT = 204;

  private static final String CHECK_ENDPOINT_TEMPLATE = "http://%s:%s/status";

  private static AgentClient agentClient;
  private static KeyValueClient keyValueClient;

  @BeforeAll
  public static void setUpClass() {
    agentClient = mock(AgentClient.class);

    Value weightValue = mock(Value.class);
    when(weightValue.getValueAsString()).thenReturn(Optional.of(Integer.toString(TEST_WEIGHT)));

    keyValueClient = mock(KeyValueClient.class);
    when(keyValueClient.getConfig()).thenReturn(new ClientConfig());
    when(keyValueClient.getEventHandler()).thenReturn(new ClientEventHandler("test", new ClientEventCallback() {}));
    when(keyValueClient.getConsulResponseWithValue(eq(String.join("/", "host", TEST_NODE_NAME, "weight")), any(QueryOptions.class)))
        .thenReturn(Optional.of(new ConsulResponse<>(weightValue, 0, true, BigInteger.ONE, null, null)));
  }

  @AfterEach
  public void tearDown() {
    reset(agentClient);
  }

  @Test
  public void testRegisterWithFullFileProperties() {
    String testCheckHost = "localhost";
    String testCheckInterval = "33s";
    String testCheckTimeout = "42s";
    int testCheckSuccessCount = 7;
    int testCheckFailCount = 8;
    String testDeregisterCriticalTimeout = "13m";
    Set<String> testTags = Set.of("tag1", "tag2");

    Properties consulProperties = new Properties();
    consulProperties.put(CONSUL_CHECK_HOST_PROPERTY, testCheckHost);
    consulProperties.put(CONSUL_CHECK_INTERVAL_PROPERTY, testCheckInterval);
    consulProperties.put(CONSUL_CHECK_TIMEOUT_PROPERTY, testCheckTimeout);
    consulProperties.put(CONSUL_CHECK_SUCCESS_COUNT_PROPERTY, Integer.toString(testCheckSuccessCount));
    consulProperties.put(CONSUL_CHECK_FAIL_COUNT_PROPERTY, Integer.toString(testCheckFailCount));
    consulProperties.put(CONSUL_DEREGISTER_CRITICAL_TIMEOUT_PROPERTY, testDeregisterCriticalTimeout);
    consulProperties.put(CONSUL_CHECK_PASSING_PROPERTY, Boolean.TRUE.toString());

    ConsulService consulService = new ConsulService(
        agentClient,
        keyValueClient,
        TEST_SERVICE_NAME,
        TEST_SERVICE_VERSION,
        TEST_NODE_NAME,
        TEST_APPLICATION_PORT,
        consulProperties,
        testTags
    );
    consulService.register();

    Registration registration = getRegistration();

    Registration.RegCheck regCheck = registration.getCheck().get();
    assertEquals(CHECK_ENDPOINT_TEMPLATE.formatted(testCheckHost, TEST_APPLICATION_PORT), regCheck.getHttp().get());
    assertEquals(testCheckInterval, regCheck.getInterval().get());
    assertEquals(testCheckTimeout, regCheck.getTimeout().get());
    assertEquals(testDeregisterCriticalTimeout, regCheck.getDeregisterCriticalServiceAfter().get());
    assertEquals(testCheckSuccessCount, regCheck.getSuccessBeforePassing().get());
    assertEquals(testCheckFailCount, regCheck.getFailuresBeforeCritical().get());
    assertEquals(Optional.of(PASSING_STATUS), regCheck.getStatus());

    ServiceWeights serviceWeights = registration.getServiceWeights().get();
    assertEquals(TEST_WEIGHT, serviceWeights.getPassing());
    assertEquals(0, serviceWeights.getWarning());

    assertEquals(String.join("-", TEST_SERVICE_NAME, TEST_NODE_NAME, Integer.toString(TEST_APPLICATION_PORT)), registration.getId());
    assertEquals(TEST_SERVICE_NAME, registration.getName());
    assertEquals(TEST_APPLICATION_PORT, registration.getPort().get());
    List<String> tags = registration.getTags();
    assertEquals(testTags.size(), tags.size());
    assertTrue(testTags.containsAll(tags));
    Map<String, String> meta = registration.getMeta();
    assertEquals(1, meta.size());
    assertEquals(Map.of(META_SERVICE_VERSION_KEY, TEST_SERVICE_VERSION), meta);
  }

  @Test
  public void testRegisterWithDefault() {
    ConsulService consulService = new ConsulService(
        agentClient,
        keyValueClient,
        TEST_SERVICE_NAME,
        TEST_SERVICE_VERSION,
        TEST_NODE_NAME,
        TEST_APPLICATION_PORT,
        new Properties(),
        Set.of()
    );
    consulService.register();

    Registration registration = getRegistration();

    Registration.RegCheck regCheck = registration.getCheck().get();
    assertEquals(CHECK_ENDPOINT_TEMPLATE.formatted(DEFAULT_CHECK_HOST, TEST_APPLICATION_PORT), regCheck.getHttp().get());
    assertEquals(DEFAULT_CHECK_INTERVAL, regCheck.getInterval().get());
    assertEquals(DEFAULT_CHECK_TIMEOUT, regCheck.getTimeout().get());
    assertEquals(DEFAULT_DEREGISTER_CRITICAL_TIMEOUT, regCheck.getDeregisterCriticalServiceAfter().get());
    assertEquals(DEFAULT_CHECK_SUCCESS_COUNT, regCheck.getSuccessBeforePassing().get());
    assertEquals(DEFAULT_CHECK_FAIL_COUNT, regCheck.getFailuresBeforeCritical().get());
    assertEquals(Optional.empty(), regCheck.getStatus());

    ServiceWeights serviceWeights = registration.getServiceWeights().get();
    assertEquals(TEST_WEIGHT, serviceWeights.getPassing());
    assertEquals(0, serviceWeights.getWarning());

    assertEquals(String.join("-", TEST_SERVICE_NAME, TEST_NODE_NAME, Integer.toString(TEST_APPLICATION_PORT)), registration.getId());
    assertEquals(TEST_SERVICE_NAME, registration.getName());
    assertEquals(TEST_APPLICATION_PORT, registration.getPort().get());
    List<String> tags = registration.getTags();
    assertEquals(0, tags.size());
    Map<String, String> meta = registration.getMeta();
    assertEquals(1, meta.size());
    assertEquals(Map.of(META_SERVICE_VERSION_KEY, TEST_SERVICE_VERSION), meta);
  }

  private Registration getRegistration() {
    ArgumentCaptor<Registration> registrationArgument = ArgumentCaptor.forClass(Registration.class);
    verify(agentClient).register(registrationArgument.capture(), any(QueryOptions.class));
    return registrationArgument.getValue();
  }
}
