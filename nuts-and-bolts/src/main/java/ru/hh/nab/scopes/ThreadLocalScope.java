package ru.hh.nab.scopes;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import java.util.HashMap;
import java.util.Map;

public class ThreadLocalScope {
  public static final Scope THREAD_LOCAL =
    new Scope() {
      public <T> Provider<T> scope(final Key<T> key, final Provider<T> creator) {
        return new Provider<T>() {
          public T get() {
            ThreadLocalCache cache = ThreadLocalCache.getInstance();
            T value = cache.get(key);
            if (value == null) {
              value = creator.get();
              cache.add(key, value);
            }
            return value;
          }
        };
      }
    };

  private static final class ThreadLocalCache {
    private static final ThreadLocal<ThreadLocalCache> THREAD_LOCAL_SCOPED =
      new ThreadLocal<ThreadLocalCache>() {
        @Override
        protected ThreadLocalCache initialValue() {
          return new ThreadLocalCache();
        }
      };

    private Map<Key<?>, Object> map = new HashMap<Key<?>, Object>();

    @SuppressWarnings("unchecked")
    public <T> T get(Key<T> key) {
      return (T) map.get(key);
    }

    public <T> void add(Key<T> key, T value) {
      map.put(key, value);
    }

    public static ThreadLocalCache getInstance() {
      return THREAD_LOCAL_SCOPED.get();
    }
  }
}
