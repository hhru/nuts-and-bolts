package ru.hh.nab.grizzly;

import org.glassfish.grizzly.memory.AbstractMemoryManager;
import org.glassfish.grizzly.memory.ByteBufferManager;
import org.glassfish.grizzly.memory.HeapMemoryManager;
import org.glassfish.grizzly.memory.MemoryManager;

import java.util.Properties;

import static ru.hh.nab.Settings.getBoolProperty;
import static ru.hh.nab.Settings.getIntProperty;

final class MemoryManagerFactory {

  static MemoryManager create(Properties memoryManagerProps) {

    String memoryManagerClass = memoryManagerProps.getProperty("class", ByteBufferManager.class.getName());
    int maxBufferSize = getIntProperty(memoryManagerProps, "maxBufferSize", AbstractMemoryManager.DEFAULT_MAX_BUFFER_SIZE);

    if (memoryManagerClass.equals(ByteBufferManager.class.getName())) {
      boolean isDirect = getBoolProperty(memoryManagerProps, "direct", true);
      int maxSmallBufferSize =
        getIntProperty(memoryManagerProps, "maxSmallBufferSize", ByteBufferManager.DEFAULT_SMALL_BUFFER_SIZE);
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
