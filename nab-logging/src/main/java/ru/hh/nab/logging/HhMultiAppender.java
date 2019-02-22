package ru.hh.nab.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import ru.hh.nab.logging.layout.StructuredJsonLayout;
import static java.util.Optional.ofNullable;

@SuppressWarnings({"rawtypes", "unchecked"})
public class HhMultiAppender extends AppenderBase {

  public HhMultiAppender() {
  }

  public HhMultiAppender(boolean json) {
    this.json = json;
  }

  public static final String LOG_TO_CONSOLE_PROPERTY_KEY = "log.toConsole";
  public static final String LOG_PATTERN_PROPERTY_KEY = "log.pattern";

  protected Appender appender;
  protected Layout<?> layout;
  protected String pattern;
  protected boolean json;

  @Override
  public void start() {
    appender = createAppender().configureAndGet();
    super.start();
  }

  @Override
  public void doAppend(Object eventObject) {
    appender.doAppend(eventObject);
  }

  @Override
  protected void append(Object eventObject) {
    throw new UnsupportedOperationException("method should never be called");
  }

  public void setLayout(Layout layout) {
    this.layout = layout;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public void setJson(boolean json) {
    this.json = json;
  }

  protected AppenderConfigurer createAppender() {
    boolean logToConsole = Boolean.parseBoolean(getContext().getProperty(LOG_TO_CONSOLE_PROPERTY_KEY));
    if (logToConsole) {
      return new AppenderConfigurer<>(new ConsoleAppender<>(), this) {
        @Override
        protected void setLayout(ConsoleAppender<Object> appender, Layout<?> layout) {
          var encoder = new LayoutWrappingEncoder();
          encoder.setLayout(layout);
          initIfNeeded(encoder, context);
          appender.setEncoder(encoder);
        }
      };
    }

    String host = getContext().getProperty(HhSyslogAppender.SYSLOG_HOST_PROPERTY_KEY);
    String port = getContext().getProperty(HhSyslogAppender.SYSLOG_PORT_PROPERTY_KEY);
    if (StringUtils.isNotEmpty(host) && StringUtils.isNumeric(port)) {
      return new AppenderConfigurer<>(new HhSyslogAppender(), this) {
        @Override
        protected void setLayout(HhSyslogAppender appender, Layout<?> layout) {
          appender.setLayout(layout);
        }
      };
    }

    return new AppenderConfigurer<>(new HhRollingAppender(), this) {
      @Override
      protected void setLayout(HhRollingAppender appender, Layout<?> layout) {
        var encoder = new LayoutWrappingEncoder();
        encoder.setLayout(layout);
        initIfNeeded(encoder, context);
        appender.setEncoder(encoder);
      }
    };
  }

  private abstract static class AppenderConfigurer<T extends Appender<?>> {
    private final T appender;
    private final HhMultiAppender optionsHolder;

    private AppenderConfigurer(T appender, HhMultiAppender optionsHolder) {
      this.appender = appender;
      this.optionsHolder = optionsHolder;
    }

    public Appender configureAndGet() {
      appender.setName(optionsHolder.getName());
      setLayout(appender, buildLayout(optionsHolder));
      initIfNeeded(appender, optionsHolder.getContext());
      return appender;
    }

    protected abstract void setLayout(T appender, Layout<?> layout);

    private static Layout<?> buildLayout(HhMultiAppender optionsHolder) {
      Layout<?> layout = Optional.<Layout<?>>ofNullable(optionsHolder.layout).orElseGet(() -> {
        if (optionsHolder.json) {
          return createJsonLayout();
        } else {
          return createPatternLayout(optionsHolder);
        }
      });
      initIfNeeded(layout, optionsHolder.getContext());
      return layout;
    }

    private static PatternLayout createPatternLayout(HhMultiAppender optionsHolder) {
      return ofNullable(optionsHolder.pattern).or(() -> ofNullable(optionsHolder.getContext().getProperty(LOG_PATTERN_PROPERTY_KEY)))
        .map(pattern -> {
          var layout = new PatternLayout();
          layout.setPattern(pattern);
          return layout;
        //need to throw Error because logback logs and ignores any Exception type
        }).orElseThrow(() -> new AssertionError("Pattern must be set via " + LOG_PATTERN_PROPERTY_KEY + " or via 'pattern' appender property"));
    }

    private static StructuredJsonLayout createJsonLayout() {
      var jsonLayout = new StructuredJsonLayout();
      jsonLayout.setIncludeContextName(false);
      return jsonLayout;
    }
  }

  private static <T extends LifeCycle&ContextAware> void initIfNeeded(T configItem, Context context) {
    if (configItem.getContext() == null) {
      configItem.setContext(context);
    }
    if (!configItem.isStarted()) {
      configItem.start();
    }
    context.register(configItem);
  }
}
