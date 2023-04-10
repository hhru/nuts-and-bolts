package ru.hh.nab.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import ru.hh.nab.performance.variants.GenericCache;
import ru.hh.nab.performance.variants.PartiallyOverflowingCache;
import ru.hh.nab.performance.variants.PartiallyOverflowingCacheOptional;
import ru.hh.nab.performance.variants.PartiallyOverflowingCacheWithSizeAtomicCache;
import ru.hh.nab.performance.variants.PartiallyOverflowingCacheWithSizeCache;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 4)
public class PartiallyOverflowingCachePerformanceTest {
  private final List<Class<?>> dataMap = createClassData();
  private final int dataSize = dataMap.size();
  private final int strongCollectionSize = dataSize / 3;
  private final int dataSetLoopSize = 4;

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(PartiallyOverflowingCachePerformanceTest.class.getSimpleName())
        .forks(1)
        .build();
    new Runner(opt).run();
  }

  ExecutorService executorService = Executors.newFixedThreadPool(8);

  @Benchmark
  public void partiallyOverflowingCacheBigTest() {
    PartiallyOverflowingCache<Class<?>, Object> doubleStorageCache =
        new PartiallyOverflowingCache<>(dataSize);
    process(doubleStorageCache);
  }

  @Benchmark
  public void partiallyOverflowingCacheOptionalBigTest() {
    PartiallyOverflowingCacheOptional<Class<?>, Object> doubleStorageCache =
        new PartiallyOverflowingCacheOptional<>(dataSize);
    process(doubleStorageCache);
  }

  @Benchmark
  public void partiallyOverflowingCacheWithSizeCacheBigTest() {
    PartiallyOverflowingCacheWithSizeCache<Class<?>, Object> doubleStorageCache =
        new PartiallyOverflowingCacheWithSizeCache<>(dataSize);
    process(doubleStorageCache);
  }

  @Benchmark
  public void partiallyOverflowingCacheWithSizeAtomicCacheBigTest() {
    PartiallyOverflowingCacheWithSizeAtomicCache<Class<?>, Object> doubleStorageCache =
        new PartiallyOverflowingCacheWithSizeAtomicCache<>(dataSize);
    process(doubleStorageCache);
  }


  @Benchmark
  public void partiallyOverflowingCacheTest() {
    PartiallyOverflowingCache<Class<?>, Object> doubleStorageCache =
        new PartiallyOverflowingCache<>(strongCollectionSize);
    process(doubleStorageCache);
  }

  @Benchmark
  public void partiallyOverflowingCacheOptionalTest() {
    PartiallyOverflowingCacheOptional<Class<?>, Object> doubleStorageCache =
        new PartiallyOverflowingCacheOptional<>(strongCollectionSize);
    process(doubleStorageCache);
  }

  @Benchmark
  public void partiallyOverflowingCacheWithSizeCacheTest() {
    PartiallyOverflowingCacheWithSizeCache<Class<?>, Object> doubleStorageCache =
        new PartiallyOverflowingCacheWithSizeCache<>(strongCollectionSize);
    process(doubleStorageCache);
  }

  @Benchmark
  public void partiallyOverflowingCacheWithSizeAtomicCacheTest() {
    PartiallyOverflowingCacheWithSizeAtomicCache<Class<?>, Object> doubleStorageCache =
        new PartiallyOverflowingCacheWithSizeAtomicCache<>(strongCollectionSize);
    process(doubleStorageCache);
  }

  @Benchmark
  public void concurrentHashMapTest() {
    ConcurrentHashMap<Class<?>, Object> concurrentHashMap = new ConcurrentHashMap<>();
    process(concurrentHashMap);
  }

  private void process(GenericCache<Class<?>, Object> data) {
    List<Callable<Object>> tasks = new ArrayList<>();
    for (int i = 0; i < dataSetLoopSize; i++) {
      for (Class<?> key : dataMap) {
        tasks.add(() -> data.computeIfAbsent(key, k -> computeNewObject()));
      }
    }
    try {
      executorService.invokeAll(tasks);
    } catch (InterruptedException e) {

    }
  }

  private void process(ConcurrentHashMap<Class<?>, Object> data) {
    List<Callable<Object>> tasks = new ArrayList<>();
    for (int i = 0; i < dataSetLoopSize; i++) {
      for (Class<?> key : dataMap) {
        tasks.add(() -> data.computeIfAbsent(key, k -> computeNewObject()));
      }
    }
    try {
      executorService.invokeAll(tasks);
    } catch (InterruptedException e) {

    }
  }

  @TearDown
  public void stop() {
    executorService.shutdownNow();
  }

  private List<Class<?>> createClassData() {
    Reflections reflectionsRu = new Reflections("ru", new SubTypesScanner(false));
    final Set<String> allTypes = reflectionsRu.getAllTypes();
    return allTypes.stream()
        .map(PartiallyOverflowingCachePerformanceTest::toClass)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private static Class<?> toClass(String s) {
    try {
      return Class.forName(s);
    } catch (Throwable e) {
      return null;
    }
  }

  private Object computeNewObject() {
    try {
      Thread.sleep(2);
    } catch (InterruptedException e) {
      return new Object();
    }
    return new Object();
  }
}
