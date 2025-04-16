package ru.hh.nab.jpa.model.test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TestConverter implements AttributeConverter<String, String> {

  @Override
  public String convertToDatabaseColumn(String value) {
    return value;
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    return dbData;
  }
}
