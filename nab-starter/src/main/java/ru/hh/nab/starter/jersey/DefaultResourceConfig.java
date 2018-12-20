package ru.hh.nab.starter.jersey;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import ru.hh.nab.starter.exceptions.AnyExceptionMapper;
import ru.hh.nab.starter.exceptions.IllegalArgumentExceptionMapper;
import ru.hh.nab.starter.exceptions.IllegalStateExceptionMapper;
import ru.hh.nab.starter.exceptions.NotFoundExceptionMapper;
import ru.hh.nab.starter.exceptions.SecurityExceptionMapper;
import ru.hh.nab.starter.exceptions.WebApplicationExceptionMapper;
import ru.hh.nab.starter.filters.ResourceNameLoggingFilter;

import java.util.Collections;

public final class DefaultResourceConfig extends ResourceConfig {

  public DefaultResourceConfig() {
    addProperties(Collections.singletonMap(ServerProperties.WADL_FEATURE_DISABLE, Boolean.TRUE));

    register(MarshallerContextResolver.class);
    register(EofSuppressingWriterInterceptor.class);

    register(AnyExceptionMapper.class);
    register(IllegalArgumentExceptionMapper.class);
    register(IllegalStateExceptionMapper.class);
    register(NotFoundExceptionMapper.class);
    register(SecurityExceptionMapper.class);
    register(WebApplicationExceptionMapper.class);

    register(ResourceNameLoggingFilter.class);
  }
}
