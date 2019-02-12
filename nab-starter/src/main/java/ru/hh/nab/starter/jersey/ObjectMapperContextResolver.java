package ru.hh.nab.starter.jersey;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Priority;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import static ru.hh.nab.starter.exceptions.NabExceptionMapper.LOW_PRIORITY;

@Provider
@Priority(LOW_PRIORITY)
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  static {
    OBJECT_MAPPER.getFactory().setCharacterEscapes(new JsonCharacterEscapes());
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return OBJECT_MAPPER;
  }
}
