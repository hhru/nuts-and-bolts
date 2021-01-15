package ru.hh.nab.starter.spring;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.ConsulService;

public final class ConsulEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        FileSettings fileSettings = context.getBeanFactory().getBean(FileSettings.class);
        return fileSettings.getBoolean(ConsulService.CONSUL_ENABLED_PROPERTY, true);
    }
}
