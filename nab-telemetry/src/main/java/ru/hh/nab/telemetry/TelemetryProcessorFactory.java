package ru.hh.nab.telemetry;

import ru.hh.jclient.common.RequestDebug;

public interface TelemetryProcessorFactory {
  RequestDebug createRequestDebug();

}
