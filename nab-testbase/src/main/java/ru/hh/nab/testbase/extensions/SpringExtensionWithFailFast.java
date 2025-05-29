package ru.hh.nab.testbase.extensions;

import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.function.ThrowingConsumer;

// TODO: rm in https://jira.hh.ru/browse/HH-262027
public class SpringExtensionWithFailFast extends SpringExtension implements ExecutionCondition {
  private static final ExtensionContext.Namespace LOCAL_NAMESPACE = ExtensionContext.Namespace.create(SpringExtensionWithFailFast.class);
  private static final ExtensionContext.Namespace PARENT_NAMESPACE = ExtensionContext.Namespace.create(SpringExtension.class);

  private static final String THE_FIRST_LAUNCH_KEY = "THE_FIRST_LAUNCH";
  private static final String LISTENERS_KEY = "LISTENERS";

  private static boolean springContextFailed = false;

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    // this method is entrypoint to extension if test instance lifecycle = per_method
    executePossibleEntryPoint(context, super::beforeAll);
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    executeIfNotExecutedBefore(context, super::beforeEach);
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) throws Exception {
    executeIfNotExecutedBefore(context, super::beforeTestExecution);
  }

  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    executeIfNotExecutedBefore(context, super::afterTestExecution);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    executeIfNotExecutedBefore(context, super::afterEach);
  }

  @Override
  public void afterAll(final ExtensionContext context) throws Exception {
    executeIfNotExecutedBefore(context, super::afterAll);
  }

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
    // skip test if context failed
    Assumptions.assumeFalse(springContextFailed, "Spring context failed to load");

    try {
      // this method is entrypoint to extension if test instance lifecycle = per_class
      executePossibleEntryPoint(context, (c) -> super.postProcessTestInstance(testInstance, context));
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

  private void executePossibleEntryPoint(ExtensionContext context, ThrowingConsumer<ExtensionContext> action) throws Exception {
    ExtensionContext.Store store = context.getStore(LOCAL_NAMESPACE);
    boolean realFirstLaunch = getTestContextManager(context) == null;
    boolean theFirstLaunch = store.getOrComputeIfAbsent(THE_FIRST_LAUNCH_KEY, ignored -> realFirstLaunch, Boolean.class);
    if (realFirstLaunch) {
      action.acceptWithException(context);
      storeListenersToContextAndClearGlobal(context, store);
    } else if (theFirstLaunch) {
      executeWithListeners(context, action, store);
    }
  }

  private static void storeListenersToContextAndClearGlobal(ExtensionContext context, ExtensionContext.Store store) {
    TestContextManager testContextManager = getTestContextManager(context);
    List<TestExecutionListener> copiedListeners = List.copyOf(testContextManager.getTestExecutionListeners());
    store.put(LISTENERS_KEY, copiedListeners);
    testContextManager.getTestExecutionListeners().clear();
  }

  private static void executeIfNotExecutedBefore(ExtensionContext context, ThrowingConsumer<ExtensionContext> action) throws Exception {
    ExtensionContext.Store store = context.getStore(LOCAL_NAMESPACE);
    boolean theFirstLaunch = Boolean.TRUE.equals(store.get(THE_FIRST_LAUNCH_KEY, Boolean.class));
    if (theFirstLaunch) {
      executeWithListeners(context, action, store);
    }
  }

  private static void executeWithListeners(
      ExtensionContext context,
      ThrowingConsumer<ExtensionContext> action,
      ExtensionContext.Store store
  ) throws Exception {
    TestContextManager testContextManager = getTestContextManager(context);
    if (testContextManager == null) {
      action.acceptWithException(context);
      return;
    }

    List<TestExecutionListener> listeners = store.get(LISTENERS_KEY, List.class);
    if (listeners != null && !listeners.isEmpty()) {
      testContextManager.getTestExecutionListeners().addAll(listeners);
    }
    try {
      action.acceptWithException(context);
    } finally {
      testContextManager.getTestExecutionListeners().clear();
    }
  }

  private static TestContextManager getTestContextManager(ExtensionContext context) {
    return context
        .getRoot()
        .getStore(PARENT_NAMESPACE)
        .get(context.getRequiredTestClass(), TestContextManager.class);
  }
}
