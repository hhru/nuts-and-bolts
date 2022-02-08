package ru.hh.nab.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Iterator;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

public class KafkaTelemetryPropagator {
  private final TextMapPropagator textMapPropagator;

  private static final TextMapGetter<Headers> GETTER = createGetter();
  private static final TextMapSetter<Headers> SETTER = createSetter();

  public KafkaTelemetryPropagator(OpenTelemetry openTelemetry) {
    this.textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
  }

  public Context getTelemetryContext(Context context, Headers carrier) {
    return textMapPropagator.extract(context, carrier, GETTER);
  }

  public void propagate(Headers carrier) {
    textMapPropagator.inject(Context.current(), carrier, SETTER);
  }

  private static TextMapSetter<Headers> createSetter() {
    return (carrier, key, value) -> carrier.add(key, value.getBytes());
  }

  private static TextMapGetter<Headers> createGetter() {
    return new TextMapGetter<>() {
      @Override
      public Iterable<String> keys(Headers carrier) {
        return new HeaderKeys(carrier);
      }

      @Override
      public String get(Headers carrier, String key) {
        Iterator<Header> headers = carrier.headers(key).iterator();
        if (!headers.hasNext()) {
          return null;
        }

        return new String(headers.next().value());
      }
    };
  }

  public static class HeaderKeys implements Iterable<String> {
    private final Headers headers;

    public HeaderKeys(Headers headers) {
      this.headers = headers;
    }

    @Override
    public Iterator<String> iterator() {
      return new HeaderKeysIterator(headers.iterator());
    }
  }

  public static class HeaderKeysIterator implements Iterator<String> {
    private final Iterator<Header> headers;

    public HeaderKeysIterator(Iterator<Header> headers) {
      this.headers = headers;
    }

    @Override
    public boolean hasNext() {
      return headers.hasNext();
    }

    @Override
    public String next() {
      return headers.next().key();
    }

    @Override
    public void remove() {
      headers.remove();
    }
  }
}
