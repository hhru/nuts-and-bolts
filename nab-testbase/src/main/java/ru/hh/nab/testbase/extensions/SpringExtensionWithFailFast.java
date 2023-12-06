package ru.hh.nab.testbase.extensions;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class SpringExtensionWithFailFast extends SpringExtension implements ExecutionCondition {
  private static boolean springContextFailed = false;

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
    // skip test if context failed
    Assumptions.assumeFalse(springContextFailed, "Spring context failed to load");

    try {
      super.postProcessTestInstance(testInstance, context);
    } catch (Exception e) {
      springContextFailed = true;
      throw e;
    }
  }

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    // skip test if context failed
    if (springContextFailed) {
      return ConditionEvaluationResult.disabled("Spring context failed to load");
    }

    return ConditionEvaluationResult.enabled(null);
  }
}
