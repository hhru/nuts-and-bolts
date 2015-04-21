package ru.hh.nab.grizzly;

import org.glassfish.grizzly.memory.AbstractMemoryManager;
import org.glassfish.grizzly.memory.ByteBufferManager;
import org.glassfish.grizzly.memory.HeapMemoryManager;
import org.glassfish.grizzly.memory.MemoryManager;

import java.util.Properties;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;

final class MemoryManagerFactory {

  static MemoryManager create(Properties memoryManagerProps) {

    String memoryManagerClass = memoryManagerProps.getProperty("class", ByteBufferManager.class.getName());
    int maxBufferSize = parseInt(
        memoryManagerProps.getProperty("maxBufferSize", String.valueOf(AbstractMemoryManager.DEFAULT_MAX_BUFFER_SIZE))
    );

    if (memoryManagerClass.equals(ByteBufferManager.class.getName())) {
      boolean isDirect = parseBoolean(
          memoryManagerProps.getProperty("direct", "true")
      );
      int maxSmallBufferSize = parseInt(
          memoryManagerProps.getProperty("maxSmallBufferSize", String.valueOf(ByteBufferManager.DEFAULT_SMALL_BUFFER_SIZE))
      );
      return new ByteBufferManager(isDirect, maxBufferSize, maxSmallBufferSize);

    } else if (memoryManagerClass.equals(HeapMemoryManager.class.getName())) {
      return new HeapMemoryManager(maxBufferSize);

    } else {
      throw new IllegalArgumentException(
          String.format(
              "unknown memory manager type '%s': must be one of '%s' or '%s'",
              memoryManagerClass, ByteBufferManager.class.getName(), HeapMemoryManager.class.getName()
          )
      );
    }
  }

  private MemoryManagerFactory() {
  }
}
