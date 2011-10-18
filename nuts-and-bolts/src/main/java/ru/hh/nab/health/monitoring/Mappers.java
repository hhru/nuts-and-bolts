package ru.hh.nab.health.monitoring;

public abstract class Mappers {
  public static BucketMapper<Integer> eqMapper(final int max) {
    return new BucketMapper<Integer>() {
      @Override
      public int bucket(Integer value) {
        return value;
      }

      @Override
      public int max() {
        return max;
      }
    };
  }
}
