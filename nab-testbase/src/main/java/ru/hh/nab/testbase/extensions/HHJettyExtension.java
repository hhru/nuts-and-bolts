package ru.hh.nab.testbase.extensions;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedFields;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.ReflectionUtils;
import static org.junit.platform.commons.util.ReflectionUtils.isPrivate;
import static org.junit.platform.commons.util.ReflectionUtils.makeAccessible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.testbase.ResourceHelper;

public class HHJettyExtension implements BeforeEachCallback, ParameterResolver {
  private static final Namespace NAMESPACE = Namespace.create(HHJettyExtension.class);
  private static final Logger LOGGER = LoggerFactory.getLogger(HHJettyExtension.class);

  private static final ConcurrentMap<Integer, JettyServer> SERVERS = new ConcurrentHashMap<>();

  @Override
  public void beforeEach(ExtensionContext context) {
    context.getRequiredTestInstances().getAllInstances()
        .forEach(instance -> injectInstanceFields(context, instance));
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    boolean annotated = parameterContext.isAnnotated(HHJetty.class);
    if (annotated && parameterContext.getDeclaringExecutable() instanceof Constructor) {
      throw new ParameterResolutionException(
          "@HHJetty is not supported on constructor parameters. Please use field injection instead.");
    }
    return annotated;
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    Class<?> parameterType = parameterContext.getParameter().getType();
    extensionContext.getRequiredTestClass();
    assertSupportedType("parameter", parameterType);
    HHJetty annotation = parameterContext.getParameter().getAnnotation(HHJetty.class);
    JettyServer jettyServer = getJettyInstanceForPort(extensionContext, getOverrideNabApplication(annotation), annotation.port());
    if (parameterType == JettyServer.class) {
      return jettyServer;
    } else if (parameterType == ResourceHelper.class) {
      return new ResourceHelper(jettyServer);
    }
    throw new IllegalArgumentException();
  }

  @NotNull
  private OverrideNabApplication getOverrideNabApplication(HHJetty annotation) {
    Class<? extends OverrideNabApplication> aClass = annotation.overrideApplication();
    if (aClass.isInterface()) {
      return new OverrideNabApplication() {
      };
    }
    try {
      return aClass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Failed to initiate OverrideNabApplication instance!");
    }
  }

  private void injectInstanceFields(ExtensionContext context, Object instance) {
    injectFields(context, instance, instance.getClass(), ReflectionUtils::isNotStatic);
  }

  private void injectFields(ExtensionContext context, Object testInstance,
                            Class<?> testClass, Predicate<Field> predicate) {
    findAnnotatedFields(testClass, HHJetty.class, predicate).forEach(field -> {
      assertValidFieldCandidate(field);
      try {
        HHJetty annotation = field.getAnnotation(HHJetty.class);
        JettyServer jettyServer = getJettyInstanceForPort(context, getOverrideNabApplication(annotation), annotation.port());
        if (field.getType() == JettyServer.class) {
          makeAccessible(field).set(testInstance, jettyServer);
        } else if (field.getType() == ResourceHelper.class) {
          makeAccessible(field).set(testInstance, new ResourceHelper(jettyServer));
        } else {
          throw new IllegalArgumentException();
        }
      } catch (Throwable t) {
        ExceptionUtils.throwAsUncheckedException(t);
      }
    });
  }

  private JettyServer getJettyInstanceForPort(ExtensionContext context, OverrideNabApplication application, int port) {
    WebApplicationContext webApplicationContext = (WebApplicationContext) SpringExtension.getApplicationContext(context);
    return SERVERS.compute(port, (key, value) -> {
      if (value != null) {
        LOGGER.debug("Reusing JettyTestContainer: {}", value);
        return value;
      }
      LOGGER.info("Creating new JettyTestContainer...");
      return application.getNabApplication()
          .run(webApplicationContext, false, serverCreateFunction -> serverCreateFunction.apply(port), false);
    });
  }

  private void assertValidFieldCandidate(Field field) {
    assertSupportedType("field", field.getType());
    if (isPrivate(field)) {
      throw new ExtensionConfigurationException("@HHJetty field [" + field + "] must not be private.");
    }
  }

  private void assertSupportedType(String target, Class<?> type) {
    if (type != JettyServer.class && type != ResourceHelper.class) {
      throw new ExtensionConfigurationException("Can only resolve @HHJetty " + target + " of type "
          + JettyServer.class.getName() + " or " + ResourceHelper.class.getName() + " but was: " + type.getName());
    }
  }
}
