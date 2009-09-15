package ru.hh.dxm;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface Marshaller<T> extends Coder<T>, Decoder<T>, Detectable {
}
