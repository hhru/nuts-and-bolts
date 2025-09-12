package ru.hh.nab.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import ru.hh.nab.logging.json.NabJsonEncoder;
import ru.hh.nab.logging.json.NabJsonLayout;

public class HhMultiAppenderTest {

  @Test
  public void testIfLogToConsoleSetConsoleAppenderCreated() {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.toConsole", "true");
    context.putProperty("log.syslogHost", "localhost");
    context.putProperty("log.syslogPort", "123");
    context.putProperty("log.syslogTag", "test");
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.start();
    assertThat(multiAppender.appender, is(instanceOf(ConsoleAppender.class)));
    checkIfAppenderProperlyConfigured(multiAppender.appender);
  }

  @Test
  public void testSyslogHostAndPortSetSyslogAppenderCreated() {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.syslogHost", "localhost");
    context.putProperty("log.syslogPort", "123");
    context.putProperty("log.syslogTag", "test");
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.start();
    assertThat(multiAppender.appender, is(instanceOf(HhSyslogAppender.class)));
    checkIfAppenderProperlyConfigured(multiAppender.appender);
  }

  @Test
  public void testIfSyslogPortOnlySetRollingAppenderCreated() {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.syslogPort", "123");
    context.putProperty("log.syslogTag", "test");
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.start();
    assertThat(multiAppender.appender, is(instanceOf(HhRollingAppender.class)));
    checkIfAppenderProperlyConfigured(multiAppender.appender);
  }

  @Test
  public void testIfSyslogPortSetWrongRollingAppenderCreated() {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.syslogHost", "localhost");
    context.putProperty("log.syslogPort", "abc");
    context.putProperty("log.syslogTag", "test");
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.start();
    assertThat(multiAppender.appender, is(instanceOf(HhRollingAppender.class)));
    checkIfAppenderProperlyConfigured(multiAppender.appender);
  }

  @Test
  public void testIfJsonUnSetPatternStructuredLayoutSet() throws Exception {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.start();
    checkIfAppenderProperlyConfigured(multiAppender.appender);
    ThrowableSupplier layoutSupplier = createLayoutSupplier(multiAppender.appender);
    assertThat(layoutSupplier.get(), is(instanceOf(PatternLayout.class)));
  }

  @Test
  public void testIfPatternInAppenderOverridesContext() throws Exception {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    String pattern = "%msg%n";
    multiAppender.setPattern(pattern);
    multiAppender.start();
    checkIfAppenderProperlyConfigured(multiAppender.appender);
    ThrowableSupplier layoutSupplier = createLayoutSupplier(multiAppender.appender);
    PatternLayout layout = (PatternLayout) layoutSupplier.get();
    assertEquals(pattern, layout.getPattern());
  }

  @Test
  public void testIfNoPatternAvailableExceptionIsThrown() {
    LoggerContext context = new LoggerContext();
    HhMultiAppender multiAppender = createHhMultiAppender(context);
    assertThrows(AssertionError.class, multiAppender::start);
  }

  @Test
  public void testIfJsonSetJsonStructuredLayoutSet() throws Exception {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.syslogHost", "localhost");
    context.putProperty("log.syslogPort", "123");
    context.putProperty("log.syslogTag", "test");
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.setJson(true);
    multiAppender.start();
    checkIfAppenderProperlyConfigured(multiAppender.appender);
    ThrowableSupplier layoutSupplier = createLayoutSupplier(multiAppender.appender);
    assertThat(layoutSupplier.get(), is(instanceOf(NabJsonLayout.class)));
  }

  @Test
  public void testJsonLogContainsAppenderField() throws Exception {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.toConsole", "true");
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.setJson(true);
    multiAppender.includeAppenderName = true;
    multiAppender.setName("service-appender");
    multiAppender.start();

    // Создаем тестовое событие
    LoggingEvent event = new LoggingEvent();
    event.setMessage("Starting HhNotesApp using Java 17.0.7 with PID 1");
    event.setLoggerName("ru.hh.notes.HhNotesApp");
    event.setLevel(ch.qos.logback.classic.Level.INFO);

    // Создаем StringWriter для захвата JSON
    StringWriter stringWriter = new StringWriter();
    
    // Получаем encoder
    ThrowableSupplier encoderSupplier = createEncoderSupplier(multiAppender.appender);
    assertNotNull(encoderSupplier);
    
    Object encoder = encoderSupplier.get();
    assertThat(encoder, is(instanceOf(NabJsonEncoder.class)));
    
    // Кодируем событие в JSON
    NabJsonEncoder nabEncoder = (NabJsonEncoder) encoder;
    byte[] jsonBytes = nabEncoder.encode(event);
    String jsonString = new String(jsonBytes);
    
    // Парсим JSON и проверяем наличие поля appender
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(jsonString);
    
    // Проверяем, что JSON содержит поле "appender"
    assertTrue(jsonNode.has("appender"), "JSON должен содержать поле 'appender'");
    assertEquals("service-appender.slog", jsonNode.get("appender").asText(), "Значение поля 'appender' должно быть 'service-appender'");
    
    // Проверяем другие поля
    assertTrue(jsonNode.has("msg"), "JSON должен содержать поле 'msg'");
    assertTrue(jsonNode.has("logger"), "JSON должен содержать поле 'logger'");
    assertTrue(jsonNode.has("lvl"), "JSON должен содержать поле 'lvl'");
    
    assertEquals("Starting HhNotesApp using Java 17.0.7 with PID 1", jsonNode.get("msg").asText());
    assertEquals("ru.hh.notes.HhNotesApp", jsonNode.get("logger").asText());
    assertEquals("INFO", jsonNode.get("lvl").asText());
    
    System.out.println("Generated JSON: " + jsonString);
  }

  @Test
  public void testJsonLogWithoutAppenderField() throws Exception {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.toConsole", "true");
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.setJson(true);
    multiAppender.includeAppenderName = false; // Не включаем appender name
    multiAppender.setName("service-appender");
    multiAppender.start();

    // Создаем тестовое событие
    LoggingEvent event = new LoggingEvent();
    event.setMessage("Starting HhNotesApp using Java 17.0.7 with PID 1");
    event.setLoggerName("ru.hh.notes.HhNotesApp");
    event.setLevel(ch.qos.logback.classic.Level.INFO);

    // Получаем encoder
    ThrowableSupplier encoderSupplier = createEncoderSupplier(multiAppender.appender);
    assertNotNull(encoderSupplier);
    
    Object encoder = encoderSupplier.get();
    assertThat(encoder, is(instanceOf(NabJsonEncoder.class)));
    
    // Кодируем событие в JSON
    NabJsonEncoder nabEncoder = (NabJsonEncoder) encoder;
    byte[] jsonBytes = nabEncoder.encode(event);
    String jsonString = new String(jsonBytes);
    
    // Парсим JSON и проверяем отсутствие поля appender
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(jsonString);
    
    // Проверяем, что JSON НЕ содержит поле "appender"
    assertFalse(jsonNode.has("appender"), "JSON не должен содержать поле 'appender' когда includeAppenderName=false");
    
    // Проверяем другие поля
    assertTrue(jsonNode.has("msg"), "JSON должен содержать поле 'msg'");
    assertTrue(jsonNode.has("logger"), "JSON должен содержать поле 'logger'");
    assertTrue(jsonNode.has("lvl"), "JSON должен содержать поле 'lvl'");
    
    System.out.println("Generated JSON without appender: " + jsonString);
  }


  @Test
  public void testNonJsonWithAppenderNameEnabled() throws Exception {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.toConsole", "true");
    context.putProperty("log.pattern", "%msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.includeAppenderName = true; // Включаем appender name
    multiAppender.setName("test-appender");
    multiAppender.start();

    // Создаем тестовое событие
    LoggingEvent event = new LoggingEvent();
    event.setMessage("Test message with appender name");
    event.setLoggerName("ru.hh.test.Logger");
    event.setLevel(ch.qos.logback.classic.Level.INFO);

    // Получаем encoder
    ThrowableSupplier encoderSupplier = createEncoderSupplier(multiAppender.appender);
    assertNotNull(encoderSupplier);
    
    Object encoder = encoderSupplier.get();
    assertThat(encoder, is(instanceOf(LayoutWrappingEncoder.class)));
    
    // Кодируем событие в текст
    LayoutWrappingEncoder<LoggingEvent> layoutEncoder = (LayoutWrappingEncoder<LoggingEvent>) encoder;
    byte[] logBytes = layoutEncoder.encode(event);
    String logOutput = new String(logBytes);
    
    // Проверяем, что вывод содержит форматирование с именем appender'а
    String expectedOutput = "[\"appender\":\"test-appender.rlog\"] Test message with appender name\n";
    assertEquals(expectedOutput, logOutput);
    
    System.out.println("Generated log with appender name: " + logOutput);
  }

  @Test
  public void testConsoleWithAppenderNameDisabled() throws Exception {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.toConsole", "true");
    context.putProperty("log.pattern", "%msg%n");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.includeAppenderName = false; // Отключаем appender name
    multiAppender.setName("test-appender");
    multiAppender.start();

    // Создаем тестовое событие
    LoggingEvent event = new LoggingEvent();
    event.setMessage("Test message without appender name");
    event.setLoggerName("ru.hh.test.Logger");
    event.setLevel(ch.qos.logback.classic.Level.INFO);

    // Получаем encoder
    ThrowableSupplier encoderSupplier = createEncoderSupplier(multiAppender.appender);
    assertNotNull(encoderSupplier);
    
    Object encoder = encoderSupplier.get();
    assertThat(encoder, is(instanceOf(LayoutWrappingEncoder.class)));
    
    // Кодируем событие в текст
    LayoutWrappingEncoder<LoggingEvent> layoutEncoder = (LayoutWrappingEncoder<LoggingEvent>) encoder;
    byte[] logBytes = layoutEncoder.encode(event);
    String logOutput = new String(logBytes);
    
    // Проверяем, что вывод НЕ содержит форматирование с именем appender'а
    String expectedOutput = "Test message without appender name\n";
    assertEquals(expectedOutput, logOutput);
    
    System.out.println("Generated log without appender name: " + logOutput);
  }

  @Test
  public void testPatternLayoutWithCustomPatternAndAppenderNameEnabled() throws Exception {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.toConsole", "true");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.setPattern("[%date] %level - %msg%n"); // Кастомный паттерн
    multiAppender.includeAppenderName = true; // Включаем appender name
    multiAppender.setName("custom-appender");
    multiAppender.start();

    // Создаем тестовое событие
    LoggingEvent event = new LoggingEvent();
    event.setMessage("Custom pattern test message");
    event.setLoggerName("ru.hh.test.Logger");
    event.setLevel(ch.qos.logback.classic.Level.INFO);

    // Получаем encoder
    ThrowableSupplier encoderSupplier = createEncoderSupplier(multiAppender.appender);
    assertNotNull(encoderSupplier);
    
    Object encoder = encoderSupplier.get();
    assertThat(encoder, is(instanceOf(LayoutWrappingEncoder.class)));
    
    // Кодируем событие в текст
    LayoutWrappingEncoder<LoggingEvent> layoutEncoder = (LayoutWrappingEncoder<LoggingEvent>) encoder;
    byte[] logBytes = layoutEncoder.encode(event);
    String logOutput = new String(logBytes);
    
    // Проверяем, что вывод содержит форматирование с именем appender'а и кастомным паттерном
    // Ожидаем что-то вроде: ["appender":"custom-appender"] [1970-01-01 03:00:00,000] INFO - Custom pattern test message
    assertTrue(logOutput.contains("[\"appender\":\"custom-appender.rlog\"]"), "Лог должен содержать форматирование с именем appender'а");
    assertTrue(logOutput.contains("Custom pattern test message"), "Лог должен содержать сообщение");
    assertTrue(logOutput.contains("INFO"), "Лог должен содержать уровень логирования");
    
    System.out.println("Generated log with custom pattern and appender name: " + logOutput);
  }

  @Test
  public void testPatternLayoutWithCustomPatternAndAppenderNameDisabled() throws Exception {
    LoggerContext context = new LoggerContext();
    context.putProperty("log.toConsole", "true");

    HhMultiAppender multiAppender = createHhMultiAppender(context);
    multiAppender.setPattern("[%date] %level - %msg%n"); // Кастомный паттерн
    multiAppender.includeAppenderName = false; // Отключаем appender name
    multiAppender.setName("custom-appender");
    multiAppender.start();

    // Создаем тестовое событие
    LoggingEvent event = new LoggingEvent();
    event.setMessage("Custom pattern test message");
    event.setLoggerName("ru.hh.test.Logger");
    event.setLevel(ch.qos.logback.classic.Level.INFO);

    // Получаем encoder
    ThrowableSupplier encoderSupplier = createEncoderSupplier(multiAppender.appender);
    assertNotNull(encoderSupplier);
    
    Object encoder = encoderSupplier.get();
    assertThat(encoder, is(instanceOf(LayoutWrappingEncoder.class)));
    
    // Кодируем событие в текст
    LayoutWrappingEncoder<LoggingEvent> layoutEncoder = (LayoutWrappingEncoder<LoggingEvent>) encoder;
    byte[] logBytes = layoutEncoder.encode(event);
    String logOutput = new String(logBytes);
    
    // Проверяем, что вывод НЕ содержит форматирование с именем appender'а, но содержит кастомный паттерн
    assertTrue(!logOutput.contains("[\"appender\":\"custom-appender.rlog\"]"), "Лог не должен содержать форматирование с именем appender'а");
    assertTrue(logOutput.contains("Custom pattern test message"), "Лог должен содержать сообщение");
    assertTrue(logOutput.contains("INFO"), "Лог должен содержать уровень логирования");
    
    System.out.println("Generated log with custom pattern without appender name: " + logOutput);
  }

  private static void checkIfAppenderProperlyConfigured(Appender<?> appender) {
    assertNotNull(appender.getContext(), "context is not set");
    assertTrue(appender.isStarted(), "appender is not started");
    ThrowableSupplier layoutSupplier = createLayoutSupplier(appender);
    if (layoutSupplier != null) {
      try {
        assertNotNull(layoutSupplier.get(), "layout is not set");
      } catch (Exception ignored) {
      }
    }
  }

  private static ThrowableSupplier createLayoutSupplier(Appender<?> appender) {
    ThrowableSupplier layoutSupplier = null;
    try {
      Method getLayout = appender.getClass().getMethod("getLayout");
      layoutSupplier = () -> getLayout.invoke(appender);
    } catch (NoSuchMethodException e) {
      try {
        Method getEncoder = appender.getClass().getMethod("getEncoder");
        Object encoder = getEncoder.invoke(appender);
        if (encoder instanceof LayoutWrappingEncoder) {
          Method getLayoutFromEncoder = LayoutWrappingEncoder.class.getMethod("getLayout");
          layoutSupplier = () -> getLayoutFromEncoder.invoke(encoder);
        }
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
      }
    }
    return layoutSupplier;
  }

  private static ThrowableSupplier createEncoderSupplier(Appender<?> appender) {
    try {
      Method getEncoder = appender.getClass().getMethod("getEncoder");
      return () -> getEncoder.invoke(appender);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  protected static HhMultiAppender createHhMultiAppender(LoggerContext context) {
    HhMultiAppender multiAppender = new HhMultiAppender();
    multiAppender.setName("test");
    multiAppender.setContext(context);
    return multiAppender;
  }

  @FunctionalInterface
  private interface ThrowableSupplier {
    Object get() throws Exception;
  }
}
