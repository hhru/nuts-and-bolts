package ru.hh.nab.starter.server.cache;

import java.nio.ByteBuffer;
import org.caffinitas.ohc.CacheSerializer;

class Serializer implements CacheSerializer<byte[]> {
  @Override
  public void serialize(byte[] value, ByteBuffer buf) {
    buf.put(value);
  }

  @Override
  public byte[] deserialize(ByteBuffer buf) {
    byte[] data = new byte[buf.remaining()];
    buf.get(data);
    return data;
  }

  @Override
  public int serializedSize(byte[] value) {
    return value.length;
  }
}

