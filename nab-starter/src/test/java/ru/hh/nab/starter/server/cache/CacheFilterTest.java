package ru.hh.nab.starter.server.cache;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CacheFilterTest {
  private static final Serializer SERIALIZER = new Serializer();

  @Test
  public void testPlaceholderSerializer() {
    CachedResponse response = new CachedResponse();
    byte[] data = response.getSerialized();

    ByteBuffer buffer = ByteBuffer.allocate(SERIALIZER.serializedSize(data));
    SERIALIZER.serialize(data, buffer);

    buffer.rewind();
    CachedResponse result = CachedResponse.from(SERIALIZER.deserialize(buffer));

    assertEquals(response.status, result.status);
    assertNull(response.headers);
    assertNull(response.body);
    assertTrue(response.isPlaceholder());
  }

  @Test
  public void testSerializer() {
    CachedResponse response = new CachedResponse(200, new ArrayList<>(), new byte[] {1, 2});
    response.headers.add(new Header("1", "2"));
    response.headers.add(new Header("X-Header", "что-то"));

    byte[] data = response.getSerialized();

    ByteBuffer buffer = ByteBuffer.allocate(SERIALIZER.serializedSize(data));
    SERIALIZER.serialize(data, buffer);

    buffer.rewind();
    CachedResponse result = CachedResponse.from(SERIALIZER.deserialize(buffer));

    assertEquals(response.status, result.status);
    assertEquals(response.headers, result.headers);
    assertArrayEquals(response.body, result.body);
  }
}
