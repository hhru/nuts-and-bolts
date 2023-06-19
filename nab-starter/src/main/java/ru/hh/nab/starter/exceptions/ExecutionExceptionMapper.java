package ru.hh.nab.starter.exceptions;

import jakarta.annotation.Priority;
import jakarta.ws.rs.ext.Provider;
import java.util.concurrent.ExecutionException;
import static ru.hh.nab.starter.exceptions.NabExceptionMapper.LOW_PRIORITY;

@Provider
@Priority(LOW_PRIORITY)
public class ExecutionExceptionMapper extends UnwrappingExceptionMapper<ExecutionException> {
}
