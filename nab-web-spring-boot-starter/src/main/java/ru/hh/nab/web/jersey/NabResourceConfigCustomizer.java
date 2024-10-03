package ru.hh.nab.web.jersey;

import java.util.Collection;
import java.util.Collections;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import ru.hh.nab.starter.exceptions.AnyExceptionMapper;
import ru.hh.nab.starter.exceptions.CompletionExceptionMapper;
import ru.hh.nab.starter.exceptions.ExecutionExceptionMapper;
import ru.hh.nab.starter.exceptions.IllegalArgumentExceptionMapper;
import ru.hh.nab.starter.exceptions.IllegalStateExceptionMapper;
import ru.hh.nab.starter.exceptions.NotFoundExceptionMapper;
import ru.hh.nab.starter.exceptions.SecurityExceptionMapper;
import ru.hh.nab.starter.exceptions.WebApplicationExceptionMapper;
import ru.hh.nab.starter.filters.ErrorAcceptFilter;
import ru.hh.nab.starter.filters.ResourceInformationFilter;

public class NabResourceConfigCustomizer implements ResourceConfigCustomizer {

  private final Collection<Object> components;

  public NabResourceConfigCustomizer(Collection<Object> components) {
    this.components = components;
  }

  @Override
  public void customize(ResourceConfig config) {
    config.addProperties(Collections.singletonMap(ServerProperties.WADL_FEATURE_DISABLE, Boolean.TRUE));

    config.register(AnyExceptionMapper.class);
    config.register(CompletionExceptionMapper.class);
    config.register(ExecutionExceptionMapper.class);
    config.register(IllegalArgumentExceptionMapper.class);
    config.register(IllegalStateExceptionMapper.class);
    config.register(NotFoundExceptionMapper.class);
    config.register(SecurityExceptionMapper.class);
    config.register(WebApplicationExceptionMapper.class);

    config.register(ErrorAcceptFilter.class);

    config.register(ResourceInformationFilter.class);

    components.forEach(config::register);
  }
}
