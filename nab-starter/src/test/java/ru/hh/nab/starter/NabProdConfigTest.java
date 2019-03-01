package ru.hh.nab.starter;

import com.timgroup.statsd.StatsDClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.APPEND;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.properties.PropertiesUtils.SETINGS_DIR_PROPERTY;
import static ru.hh.nab.starter.NabCommonConfig.SERVICE_NAME_PROPERTY;
import static ru.hh.nab.starter.NabProdConfig.DATACENTER_NAME_PROPERTY;

public class NabProdConfigTest {
  private static final String TEST_SERVICE_NAME = "test-service";
  private static final String TEST_DATACENTER_NAME = "test-dc";

  private Path propertiesFile;

  @Before
  public void setUp() throws Exception {
    Path tempDir = Files.createTempDirectory("");
    System.setProperty(SETINGS_DIR_PROPERTY, tempDir.toString());
    propertiesFile = createTestPropertiesFile(tempDir);
  }

  @After
  public void tearDown() throws Exception {
    System.clearProperty(SETINGS_DIR_PROPERTY);
    Files.deleteIfExists(propertiesFile);
  }

  @Test
  public void testInitContext() {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.register(NabProdConfig.class);
    context.refresh();

    assertNotNull(context.getBean(FileSettings.class));

    assertEquals(TEST_SERVICE_NAME, context.getBean(SERVICE_NAME_PROPERTY, String.class));
    assertEquals(TEST_DATACENTER_NAME, context.getBean(DATACENTER_NAME_PROPERTY, String.class));

    assertNotNull(context.getBean(StatsDClient.class));
    assertNotNull(context.getBean(StatsDSender.class));
    assertNotNull(context.getBean("cacheFilter", FilterHolder.class));
    assertNotNull(context.getBean("jettyThreadPool", ThreadPool.class));
    assertNotNull(context.getBean(ScheduledExecutorService.class));
    assertNotNull(context.getBean(AppMetadata.class));
  }

  private static Path createTestPropertiesFile(Path dir) throws IOException {
    Path propertiesFile = Files.createFile(Paths.get(dir.toString(), NabProdConfig.PROPERTIES_FILE_NAME));
    List<String> lines = new ArrayList<>();
    lines.add(String.format("%s=%s", SERVICE_NAME_PROPERTY, TEST_SERVICE_NAME));
    lines.add(String.format("%s=%s", DATACENTER_NAME_PROPERTY, TEST_DATACENTER_NAME));
    Files.write(propertiesFile, lines, APPEND);
    return propertiesFile;
  }
}
