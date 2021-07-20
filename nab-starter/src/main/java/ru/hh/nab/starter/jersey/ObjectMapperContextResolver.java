package ru.hh.nab.starter.jersey;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.ws.rs.ext.ContextResolver;

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
