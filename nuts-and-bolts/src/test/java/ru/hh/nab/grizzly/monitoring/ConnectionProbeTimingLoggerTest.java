package ru.hh.nab.grizzly.monitoring;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentMap;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.nio.transport.TCPNIOConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;

public class ConnectionProbeTimingLoggerTest {
  private Logger logger;
  private ConnectionProbeTimingLogger probe;
  private ConcurrentMap<String, Long> timingRecords;
  private int logThreshold;
  
  
  @Before
  @SuppressWarnings("unchecked")
  public void setup() throws Exception {
    logger = Mockito.mock(Logger.class);
    probe = new ConnectionProbeTimingLogger(-1, logger);
    timingRecords = (ConcurrentMap<String, Long>) Whitebox.getInternalState(probe, "timingRecords");

    setFinalStatic(ConnectionProbeTimingLogger.class.getField("LOG_THRESHOLD"), 10);
    logThreshold = (int) Whitebox.getInternalState(probe, "LOG_THRESHOLD");
  }
  
  @Test
  public void testLeak() throws Exception {    
    for (int i = 0; i < logThreshold + 1; ++i) {
      Connection connection = new TCPNIOConnection(new TCPNIOTransport(), null) {
        @Override
        public SocketAddress getPeerAddress() {
          return new SocketAddress() {};
        }
      };
      testEndUserRequest(connection);
    }
    Mockito.verify(logger, Mockito.atLeast(1))
        .warn(ConnectionProbeTimingLogger.TCP_MARKER, "timingsRecords map too large: " + String.valueOf(timingRecords.size()));
  }
  
  @Test
  public void testNormal() throws Exception {
    for (int i = 0; i < logThreshold + 1; ++i) {
      final SocketAddress address = new SocketAddress() {};
      Connection connection = new TCPNIOConnection(new TCPNIOTransport(), null) {
        @Override
        public SocketAddress getPeerAddress() {
          return address;
        }
      };
      testEndUserRequest(connection);
    }
    Mockito.verify(logger, Mockito.never())
        .warn(ConnectionProbeTimingLogger.TCP_MARKER, "timingsRecords map too large: " + String.valueOf(timingRecords.size()));
  }
  
  public void testEndUserRequest(Connection connection) throws Exception {
    try {
      probe.onAcceptEvent(null, connection);
      probe.onReadEvent(connection, null, 0);
      probe.endUserRequest("100500", connection);
      probe.onWriteEvent(connection, null, 0L);
    } finally {
      probe.onCloseEvent(connection);
    }        
  }

  static void setFinalStatic(Field field, Object newValue) throws Exception {
    field.setAccessible(true);

    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

    field.set(null, newValue);
  }
}
