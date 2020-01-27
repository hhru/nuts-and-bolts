package ru.hh.nab.testbase.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Supplier;

public interface TestObjectMapperSupplier extends Supplier<ObjectMapper> {
}
