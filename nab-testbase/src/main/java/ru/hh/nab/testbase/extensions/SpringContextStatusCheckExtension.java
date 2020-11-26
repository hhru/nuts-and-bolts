package ru.hh.nab.testbase.extensions;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;

public class SpringContextStatusCheckExtension implements TestInstancePostProcessor, ExecutionCondition {
  private static final Namespace NAMESPACE = Namespace.create(SpringExtension.class);
  private static final Map<String, Throwable> REASON_EXCEPTION = new HashMap<>();
  private static boolean showedException = false;

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
    Assert.notNull(context, "ExtensionContext must not be null");
    Class<?> testClass = context.getRequiredTestClass();
    ExtensionContext.Store store = getStore(context);
    try {
      TestContextManager contextManager = store.getOrComputeIfAbsent(testClass, TestContextManager::new, TestContextManager.class);
      contextManager.prepareTestInstance(testInstance);
    } catch (Exception e) {
      REASON_EXCEPTION.put("Spring context fail to initiate", e);
    }
  }

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    if (REASON_EXCEPTION.isEmpty()) {
      return ConditionEvaluationResult.enabled(null);
    }
    if (showedException) {
      return ConditionEvaluationResult.disabled(null);
    }
    showedException = true;
    return ConditionEvaluationResult.disabled(String.join(", ", REASON_EXCEPTION.keySet()));
  }

  private ExtensionContext.Store getStore(ExtensionContext context) {
    return context.getRoot().getStore(NAMESPACE);
  }
}
