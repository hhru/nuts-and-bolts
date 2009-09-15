package ru.hh.dxm.converters;

import com.google.common.collect.BiMap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import ru.hh.dxm.MarshalException;

public final class ValueConverters {
  private ValueConverters() {
  }

  private static final ValueConverter<Boolean> BOOLEAN_CONVERTER = new ValueConverter<Boolean>() {
    @Override
    public Boolean fromString(String s) {
      return Boolean.valueOf(s);
    }

    @Override
    public String toString(Boolean v) {
      return Boolean.toString(v);
    }
  };

  private static final ValueConverter<Byte> BYTE_CONVERTER = new ValueConverter<Byte>() {
    @Override
    public Byte fromString(String s) {
      return Byte.valueOf(s);
    }

    @Override
    public String toString(Byte v) {
      return Byte.toString(v);
    }
  };

  private static final ValueConverter<Short> SHORT_CONVERTER = new ValueConverter<Short>() {
    @Override
    public Short fromString(String s) {
      return Short.valueOf(s);
    }

    @Override
    public String toString(Short v) {
      return Short.toString(v);
    }
  };

  private static final ValueConverter<Character> CHAR_CONVERTER = new ValueConverter<Character>() {
    @Override
    public Character fromString(String s) {
      return s.charAt(0);
    }

    @Override
    public String toString(Character v) {
      return Character.toString(v);
    }
  };

  private static final ValueConverter<Integer> INT_CONVERTER = new ValueConverter<Integer>() {
    @Override
    public Integer fromString(String s) {
      return Integer.valueOf(s);
    }

    @Override
    public String toString(Integer v) {
      return Integer.toString(v);
    }
  };

  private static final ValueConverter<Long> LONG_CONVERTER = new ValueConverter<Long>() {
    @Override
    public Long fromString(String s) {
      return Long.valueOf(s);
    }

    @Override
    public String toString(Long v) {
      return Long.toString(v);
    }
  };

  private static final ValueConverter<String> STRING_CONVERTER = new ValueConverter<String>() {
    @Override
    public String fromString(String s) {
      return s;
    }

    @Override
    public String toString(String v) {
      return v;
    }
  };

  public static ValueConverter<Boolean> forBool() {
    return BOOLEAN_CONVERTER;
  }

  public static ValueConverter<Byte> forByte() {
    return BYTE_CONVERTER;
  }

  public static ValueConverter<Short> forShort() {
    return SHORT_CONVERTER;
  }

  public static ValueConverter<Character> forChar() {
    return CHAR_CONVERTER;
  }

  public static ValueConverter<Integer> forInt() {
    return INT_CONVERTER;
  }

  public static ValueConverter<Long> forLong() {
    return LONG_CONVERTER;
  }

  public static ValueConverter<String> forString() {
    return STRING_CONVERTER;
  }

  public static <T extends Enum<T>> ValueConverter<T> forEnum(final Class<T> klass) {
    return new ValueConverter<T>() {
      @Override
      public T fromString(String s) {
        return Enum.valueOf(klass, s);
      }

      @Override
      public String toString(Enum v) {
        return v.name();
      }
    };
  }

  public static ValueConverter<Date> forDate() {
    DateFormat ISO8601UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    ISO8601UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    return forDate(ISO8601UTC);
  }

  public static ValueConverter<Date> forDate(final DateFormat format) {
    return new ValueConverter<Date>() {
      @Override
      public Date fromString(String s) {
        try {
          return format.parse(s);
        } catch (ParseException e) {
          throw new MarshalException(e);
        }
      }

      @Override
      public String toString(Date v) {
        return format.format(v);
      }
    };
  }

  public static <V> ValueConverter<V> forBiMap(final BiMap<String, V> map) {
    return new ValueConverter<V>() {
      @Override
      public V fromString(String s) {
        return map.get(s);
      }

      @Override
      public String toString(V v) {
        return map.inverse().get(v);
      }
    };
  }

  public static <V, R> ValueConverter<V> forBiMap(final ValueConverter<R> representationMap, final BiMap<R, V> map) {
    return new ValueConverter<V>() {
      @Override
      public V fromString(String s) {
        return map.get(representationMap.fromString(s));
      }

      @Override
      public String toString(V v) {
        return representationMap.toString(map.inverse().get(v));
      }
    };
  }

  public static <V extends Enum<V>, R> ValueConverter<R> forBiMappedEnum(Class<V> klass, final BiMap<R, V> map) {
    return ValueConverters.forBiMap(ValueConverters.forEnum(klass), map.inverse());
  }

  public static <T> ValueConverter<T> toStringAndValueOf(Class<T> klass) {
    final Method valueOf;
    try {
      valueOf = klass.getDeclaredMethod("valueOf", String.class);
      if (!klass.isAssignableFrom(valueOf.getReturnType()))
        throw new IllegalArgumentException(
                "valueOf(String) method return type is incompatible with " + klass.getName());
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("No static T valueOf(String) method found");
    }
    return new ValueConverter<T>() {
      @SuppressWarnings({"unchecked"})
      @Override
      public T fromString(String s) {
        try {
          return (T) valueOf.invoke(null, s);
        } catch (IllegalAccessException e) {
          throw new MarshalException(e);
        } catch (InvocationTargetException e) {
          Throwable cause = e.getCause();
          if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
          }
          throw new MarshalException(e);
        }
      }

      @Override
      public String toString(T v) {
        return v.toString();
      }
    };
  }

  public static <T> ValueConverter<T> toString() {
    return new ValueConverter<T>() {
      @SuppressWarnings({"unchecked"})
      @Override
      public T fromString(String s) {
        throw new UnsupportedOperationException();
      }

      @Override
      public String toString(T v) {
        return v.toString();
      }
    };
  }
}
