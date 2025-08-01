package ru.hh.nab.web.exceptions;

import static java.util.Objects.requireNonNull;
import org.glassfish.jersey.server.internal.process.MappableException;

/**
 * Automatically unwrapped by jersey. See {@link MappableException}.
 */
public class NabMappableException extends MappableException {
  public NabMappableException(Throwable cause) {
    super("Wrapper exception for jersey mappers", requireNonNull(cause));
  }
}
