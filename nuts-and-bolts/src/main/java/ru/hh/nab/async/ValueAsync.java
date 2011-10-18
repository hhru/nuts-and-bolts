package ru.hh.nab.async;

public class ValueAsync<T> extends Async<T> {
  private final T value;

  public ValueAsync(T value) {
    this.value = value;
  }

  @Override
  protected void runExposed(Callback<T> onSuccess, Callback<Throwable> onError) throws Exception {
    onSuccess.call(value);
  }
}
