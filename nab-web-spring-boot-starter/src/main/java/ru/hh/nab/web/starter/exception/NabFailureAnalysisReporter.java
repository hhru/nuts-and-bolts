package ru.hh.nab.web.starter.exception;

import static java.text.MessageFormat.format;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalysisReporter;
import org.springframework.util.StringUtils;

public class NabFailureAnalysisReporter implements FailureAnalysisReporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(NabFailureAnalysisReporter.class);

  @Override
  public void report(FailureAnalysis analysis) {
    String message = buildMessage(analysis);
    LOGGER.error(message, analysis.getCause());
    System.err.println(format("[{0}] {1}", LocalDateTime.now(), message));
  }

  private String buildMessage(FailureAnalysis analysis) {
    StringBuilder sb = new StringBuilder();
    sb.append("Failed to start, shutting down: ");
    sb.append(analysis.getDescription());
    if (StringUtils.hasText(analysis.getAction())) {
      sb.append(" Action: ");
      sb.append(analysis.getAction());
    }
    return sb.toString();
  }
}
