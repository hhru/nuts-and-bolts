package ru.hh.nab.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import ru.hh.nab.logging.json.NabJsonEncoder;
import ru.hh.nab.logging.json.NabJsonLayout;
import static java.util.Optional.ofNullable;

public class HhMultiAppender extends AppenderBase<ILoggingEvent> {

  public HhMultiAppender() {
  }

  public HhMultiAppender(boolean json) {
    this.json = json;
  }

  public static final String LOG_TO_CONSOLE_PROPERTY_KEY = "log.toConsole";
  public static final String LOG_PATTERN_PROPERTY_KEY = "log.pattern";

  protected Appender<ILoggingEvent> appender;
  protected Supplier<Layout<ILoggingEvent>> layoutSupplier;
  protected Supplier<Encoder<ILoggingEvent>> encoderSupplier;
  protected String pattern;
  protected boolean json;

  @Override
  public void start() {
    appender = createAppender().configureAndGet();
    super.start();
  }

  @Override
  public void doAppend(ILoggingEvent eventObject) {
    appender.doAppend(eventObject);
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    throw new UnsupportedOperationException("method should never be called");
  }

  public void setLayoutSupplier(Supplier<Layout<ILoggingEvent>> layoutSupplier) {
    this.layoutSupplier = layoutSupplier;
  }

  public void setEncoderSupplier(Supplier<Encoder<ILoggingEvent>> encoderSupplier) {
    this.encoderSupplier = encoderSupplier;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public void setJson(boolean json) {
    this.json = json;
  }

  protected AppenderConfigurer<?> createAppender() {
    boolean logToConsole = Boolean.parseBoolean(getContext().getProperty(LOG_TO_CONSOLE_PROPERTY_KEY));
    if (logToConsole) {
      return new AppenderConfigurer<>(new ConsoleAppender<ILoggingEvent>(), this) {
        @Override
        protected void configure(ConsoleAppender<ILoggingEvent> appender) {
          appender.setEncoder(buildEncoder());
        }
      };
    }

    String host = getContext().getProperty(HhSyslogAppender.SYSLOG_HOST_PROPERTY_KEY);
    String port = getContext().getProperty(HhSyslogAppender.SYSLOG_PORT_PROPERTY_KEY);
    if (StringUtils.isNotEmpty(host) && StringUtils.isNumeric(port)) {
      return new AppenderConfigurer<>(new HhSyslogAppender(this.json), this) {
        @Override
        protected void configure(HhSyslogAppender appender) {
          appender.setLayout(buildLayout());
        }
      };
    }

    return new AppenderConfigurer<>(new HhRollingAppender(), this) {
      @Override
      protected void configure(HhRollingAppender appender) {
        appender.setEncoder(buildEncoder());
      }
    };
  }

  private abstract static class AppenderConfigurer<T extends Appender<ILoggingEvent>> {
    private final T appender;
    private final HhMultiAppender optionsHolder;

    private AppenderConfigurer(T appender, HhMultiAppender optionsHolder) {
      this.appender = appender;
      this.optionsHolder = optionsHolder;
    }

    protected Appender<ILoggingEvent> configureAndGet() {
      appender.setName(optionsHolder.getName());
      configure(appender);
      initIfNeeded(appender, optionsHolder.getContext());
      return appender;
    }

    protected abstract void configure(T appender);

    protected Layout<ILoggingEvent> buildLayout() {
      Layout<ILoggingEvent> layout;
      if (optionsHolder.layoutSupplier != null) {
        layout = optionsHolder.layoutSupplier.get();
      } else if (optionsHolder.json) {
        layout = new NabJsonLayout();
      } else {
        layout = createPatternLayout(optionsHolder);
      }
      initIfNeeded(layout, optionsHolder.getContext());
      return layout;
    }

    protected Encoder<ILoggingEvent> buildEncoder() {
      Encoder<ILoggingEvent> encoder;
      if (optionsHolder.encoderSupplier != null) {
        encoder = optionsHolder.encoderSupplier.get();
      } else if (optionsHolder.json) {
        encoder = new NabJsonEncoder();
      } else {
        encoder = new LayoutWrappingEncoder<>();
        ((LayoutWrappingEncoder<ILoggingEvent>) encoder).setLayout(buildLayout());
      }
      initIfNeeded(encoder, optionsHolder.getContext());
      return encoder;
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
  }

  private static <T extends LifeCycle & ContextAware> void initIfNeeded(T configItem, Context context) {
    if (configItem.getContext() == null) {
      configItem.setContext(context);
    }
    if (!configItem.isStarted()) {
      configItem.start();
    }
    context.register(configItem);
  }
}
