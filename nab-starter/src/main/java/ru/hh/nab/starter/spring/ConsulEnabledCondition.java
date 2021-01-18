package ru.hh.nab.starter.spring;

import static java.util.Optional.ofNullable;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.ConsulService;

public final class ConsulEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return ofNullable(context.getBeanFactory()).map(factory -> factory.getBean(FileSettings.class))
          .map(fileSettings -> fileSettings.getBoolean(ConsulService.CONSUL_ENABLED_PROPERTY, true)).orElse(Boolean.FALSE);
    }
}
