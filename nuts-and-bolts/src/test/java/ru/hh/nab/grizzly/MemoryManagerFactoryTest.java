package ru.hh.nab.grizzly;

import org.glassfish.grizzly.memory.ByteBufferManager;
import org.glassfish.grizzly.memory.HeapMemoryManager;
import org.glassfish.grizzly.memory.MemoryManager;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MemoryManagerFactoryTest {

  @Test
  public void shouldCreateDirectByteBufferMemoryManagerIfNoMemoryManagerClass() throws Exception {

    final Properties props = new Properties();

    final MemoryManager memoryManager = MemoryManagerFactory.create(props);

    assertTrue(memoryManager instanceof ByteBufferManager);
    ByteBufferManager byteBufferManager = (ByteBufferManager) memoryManager;
    assertTrue(byteBufferManager.isDirect());
    assertEquals(ByteBufferManager.DEFAULT_MAX_BUFFER_SIZE, byteBufferManager.getMaxBufferSize());
    assertEquals(ByteBufferManager.DEFAULT_SMALL_BUFFER_SIZE, byteBufferManager.getMaxSmallBufferSize());
  }

  @Test
  public void shouldCreateByteBufferMemoryManagerWithCustomSettings() throws Exception {

    final Properties props = new Properties();
    props.setProperty("class", ByteBufferManager.class.getName());
    props.setProperty("direct", "false");
    props.setProperty("maxBufferSize", "777");
    props.setProperty("maxSmallBufferSize", "666");

    final MemoryManager memoryManager = MemoryManagerFactory.create(props);

    assertTrue(memoryManager instanceof ByteBufferManager);
    ByteBufferManager byteBufferManager = (ByteBufferManager) memoryManager;
    assertFalse(byteBufferManager.isDirect());
    assertEquals(777, byteBufferManager.getMaxBufferSize());
    assertEquals(666, byteBufferManager.getMaxSmallBufferSize());
  }

  @Test
  public void shouldCreateHeapMemoryManagerWithDefaultSettings() throws Exception {

    final Properties props = new Properties();
    props.setProperty("class", HeapMemoryManager.class.getName());

    final MemoryManager memoryManager = MemoryManagerFactory.create(props);

    assertTrue(memoryManager instanceof HeapMemoryManager);
    HeapMemoryManager heapMemoryManager = (HeapMemoryManager) memoryManager;
    assertEquals(HeapMemoryManager.DEFAULT_MAX_BUFFER_SIZE, heapMemoryManager.getMaxBufferSize());
  }

  @Test
  public void shouldCreateHeapMemoryManagerWithCustomSettings() throws Exception {

    final Properties props = new Properties();
    props.setProperty("class", HeapMemoryManager.class.getName());
    props.setProperty("maxBufferSize", "777");

    final MemoryManager memoryManager = MemoryManagerFactory.create(props);

    assertTrue(memoryManager instanceof HeapMemoryManager);
    HeapMemoryManager heapMemoryManager = (HeapMemoryManager) memoryManager;
    assertEquals(777, heapMemoryManager.getMaxBufferSize());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentExceptionIfWrongMemoryManagerClass() throws Exception {

    final Properties props = new Properties();
    props.setProperty("class", "wrong");

    MemoryManagerFactory.create(props);
  }
}
