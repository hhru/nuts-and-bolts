package ru.hh.nab.starter;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.config.ClientConfig;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.catalog.ServiceWeights;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.orbitz.consul.monitoring.ClientEventCallback;
import com.orbitz.consul.monitoring.ClientEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.hh.nab.testbase.NabTestConfig.TEST_SERVICE_NAME;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ru.hh.nab.starter.server.jetty.JettySettingsConstants;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ConsulServiceTest.CustomKVConfig.class)
public class ConsulServiceTest {
  public static final String TEST_NODE_NAME = "testNode";

  @Autowired
  private ConsulService consulService;
  @Autowired
  private AgentClient agentClient;
  @Autowired
  private KeyValueClient keyValueClient;

  @BeforeEach
  void setUp() {

  }

  @Test
  public void testRegisterWithFullFileProperties() {
    ArgumentCaptor<Registration> argument = ArgumentCaptor.forClass(Registration.class);
    consulService.register();
    verify(agentClient).register(argument.capture());
    Registration registration = argument.getValue();

    assertTrue(registration.getCheck().isPresent());
    Registration.RegCheck regCheck = registration.getCheck().get();
    assertEquals("http://localhost:0/status", regCheck.getHttp().get());
    assertEquals("33s", regCheck.getInterval().get());
    assertEquals("42s", regCheck.getTimeout().get());
    assertEquals("13m", regCheck.getDeregisterCriticalServiceAfter().get());
    assertEquals(7, regCheck.getSuccessBeforePassing().get());
    assertEquals(8, regCheck.getFailuresBeforeCritical().get());

    assertTrue(registration.getServiceWeights().isPresent());
    ServiceWeights serviceWeights = registration.getServiceWeights().get();
    assertEquals(204, serviceWeights.getPassing());
    assertEquals(102, serviceWeights.getWarning());

    assertEquals(String.join("-", TEST_SERVICE_NAME, TEST_NODE_NAME, "0"), registration.getId());
    assertEquals("testService", registration.getName());
    assertEquals(0, registration.getPort().get());
    List<String> tags = registration.getTags();
    assertEquals(2, tags.size());
    assertEquals(List.of("tag1", "tag2"), tags);
    Map<String, String> meta = registration.getMeta();
    assertEquals(1, meta.size());
    assertEquals(Map.of("serviceVersion", "test-version"), meta);
  }

  @Test
  public void testRegisterWithDefault() {
    ArgumentCaptor<Registration> defaultArgument = ArgumentCaptor.forClass(Registration.class);

    AnnotationConfigWebApplicationContext aggregateCtx = new AnnotationConfigWebApplicationContext();
    aggregateCtx.register(EmptyConsulConfig.class);
    aggregateCtx.refresh();
    ConsulService defaultConsulService = aggregateCtx.getBean(ConsulService.class);
    AgentClient defaultAgentClient = aggregateCtx.getBean(AgentClient.class);

    defaultConsulService.register();
    verify(defaultAgentClient).register(defaultArgument.capture());
    Registration registration = defaultArgument.getValue();

    assertTrue(registration.getCheck().isPresent());
    Registration.RegCheck regCheck = registration.getCheck().get();
    assertEquals("http://127.0.0.1:17/status", regCheck.getHttp().get());
    assertEquals("5s", regCheck.getInterval().get());
    assertEquals("5s", regCheck.getTimeout().get());
    assertEquals("10m", regCheck.getDeregisterCriticalServiceAfter().get());
    assertEquals(2, regCheck.getSuccessBeforePassing().get());
    assertEquals(2, regCheck.getFailuresBeforeCritical().get());

    assertTrue(registration.getServiceWeights().isPresent());
    ServiceWeights serviceWeights = registration.getServiceWeights().get();
    assertEquals(204, serviceWeights.getPassing());
    assertEquals(68, serviceWeights.getWarning());


    assertEquals(String.join("-", "defaultTestService", TEST_NODE_NAME, "17"), registration.getId());
    assertEquals("defaultTestService", registration.getName());
    assertEquals(17, registration.getPort().get());
    List<String> tags = registration.getTags();
    assertEquals(0, tags.size());
    Map<String, String> meta = registration.getMeta();
    assertEquals(1, meta.size());
    assertEquals(Map.of("serviceVersion", "unknown"), meta);
  }

  @Configuration
  @Import(NabAppTestConfig.class)
  public static class CustomKVConfig {
    @Bean
    KeyValueClient keyValueClient() {
      KeyValueClient mock = mock(KeyValueClient.class);
      when(mock.getConfig()).thenReturn(new ClientConfig());
      when(mock.getEventHandler()).thenReturn(new ClientEventHandler("test", new ClientEventCallback() {}));
      when(mock.getValueAsString(String.join("/", "host", TEST_NODE_NAME, "weight"))).thenReturn(Optional.of("204"));
      return mock;
    }

  }

  @Configuration
  @Import(CustomKVConfig.class)
  public static class EmptyConsulConfig {

    @Bean
    @Primary
    Properties serviceProperties() {
      Properties properties = new Properties();
      properties.setProperty(ConsulService.CONSUL_REGISTRATION_ENABLED_PROPERTY, "true");
      properties.setProperty(NabCommonConfig.SERVICE_NAME_PROPERTY, "defaultTestService");
      properties.setProperty(NabCommonConfig.DATACENTER_NAME_PROPERTY, "test");
      properties.setProperty(NabCommonConfig.NODE_NAME_PROPERTY, TEST_NODE_NAME);
      properties.setProperty(JettySettingsConstants.JETTY_PORT, "17");
      return properties;
    }
  }
}
