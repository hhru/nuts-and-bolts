package ru.hh.nab.starter.server.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class CachedResponse implements Serializable {
  private static final long serialVersionUID = 4535812629333313638L;

  private static final Set<String> EXCLUDE_HEADERS =
      Stream.of("Date", "Connection", "Keep-Alive", "Public", "Upgrade", "Transfer-Encoding", "Proxy-Authenticate", "X-Request-Id")
          .map(String::toLowerCase)
          .collect(Collectors.toSet());

  private static final Logger LOGGER = LoggerFactory.getLogger(CachedResponse.class);
  static final byte[] PLACEHOLDER = new CachedResponse().getSerialized();

  public final int status;
  public final List<Header> headers;
  public final byte[] body;

  public boolean isPlaceholder() {
    return status == 0;
  }

  CachedResponse() {
    this(0, null, null);
  }

  CachedResponse(int status, List<Header> headers, byte[] body) {
    this.status = status;
    this.headers = headers;
    this.body = body;
  }

  public byte[] getSerialized() {
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream)) {
      outputStream.writeObject(this);
      outputStream.flush();

      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      LOGGER.error("CachedResponse serialization error", e);
    }

    return null;
  }

  public static CachedResponse from(CachingResponseWrapper responseWrapper) {
    List<Header> headers = new ArrayList<>();
    for (String header : responseWrapper.getHeaderNames()) {
      Collection<String> values = responseWrapper.getHeaders(header);

      if (header != null && values != null && !EXCLUDE_HEADERS.contains(header.toLowerCase())) {
        values.stream().filter(Objects::nonNull).forEach(value -> headers.add(new Header(header, value)));
      }
    }

    return new CachedResponse(responseWrapper.getStatus(), headers, responseWrapper.getContentAsByteArray());
  }

  public static CachedResponse from(byte[] data) {
    if (data == null) {
      return null;
    }

    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
         ObjectInputStream inputStream = new ObjectInputStream(byteArrayInputStream)) {
      return (CachedResponse) inputStream.readObject();
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.error("CachedResponse deserialization error", e);
    }

    return null;
  }
}

