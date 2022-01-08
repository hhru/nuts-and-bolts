package ru.hh.nab.starter.exceptions;

import java.util.concurrent.CompletionException;
import javax.annotation.Priority;
import javax.ws.rs.ext.Provider;
import static ru.hh.nab.starter.exceptions.NabExceptionMapper.LOW_PRIORITY;

@Provider
@Priority(LOW_PRIORITY)
public class CompletionExceptionMapper extends UnwrappingExceptionMapper<CompletionException> {
}
