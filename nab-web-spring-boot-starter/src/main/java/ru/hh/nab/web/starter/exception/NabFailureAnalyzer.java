package ru.hh.nab.web.starter.exception;

import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import static org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import static org.springframework.core.io.support.SpringFactoriesLoader.FailureHandler;
import static org.springframework.core.io.support.SpringFactoriesLoader.forDefaultResourceLocation;

public class NabFailureAnalyzer implements FailureAnalyzer, PriorityOrdered {

  private static final Logger LOGGER = LoggerFactory.getLogger(NabFailureAnalyzer.class);

  private final BeanFactory beanFactory;
  private final Environment environment;

  public NabFailureAnalyzer(BeanFactory beanFactory, Environment environment) {
    this.beanFactory = beanFactory;
    this.environment = environment;
  }

  @Override
  public FailureAnalysis analyze(Throwable failure) {
    List<FailureAnalyzer> analyzers = loadFailureAnalyzers()
        .stream()
        .filter(analyzer -> analyzer.getClass() != this.getClass())
        .toList();

    for (FailureAnalyzer analyzer : analyzers) {
      try {
        FailureAnalysis analysis = analyzer.analyze(failure);
        if (analysis != null) {
          return analysis;
        }
      }
      catch (Throwable ex) {
        LOGGER.trace("FailureAnalyzer {} failed", analyzer.getClass().getName(), ex);
      }
    }
    return new FailureAnalysis(ExceptionUtils.getRootCause(failure).getMessage(), null, failure);
  }

  @Override
  public int getOrder() {
    return 0;
  }

  private List<FailureAnalyzer> loadFailureAnalyzers() {
    SpringFactoriesLoader springFactoriesLoader = forDefaultResourceLocation(this.getClass().getClassLoader());
    return springFactoriesLoader.load(
        FailureAnalyzer.class,
        getArgumentResolver(),
        FailureHandler.logging(LogFactory.getLog(NabFailureAnalyzer.class))
    );
  }

  private ArgumentResolver getArgumentResolver() {
    if (beanFactory == null || environment == null) {
      return null;
    }
    ArgumentResolver argumentResolver = ArgumentResolver.of(BeanFactory.class, beanFactory);
    argumentResolver = argumentResolver.and(Environment.class, environment);
    return argumentResolver;
  }
}
