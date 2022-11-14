package ru.hh.nab.testbase.redis;

import java.util.Optional;
import org.testcontainers.containers.GenericContainer;

public class EmbeddedRedisFactory {

  private static final String DEFAULT_REDIS_IMAGE = "redis:5.0.7";
  //todo: узнать есть ли переменная, название, поменять на правильное
  private static final String REDIS_IMAGE_ENV_VARIABLE = "EXT_REDIS_IMAGE";
  public static final int REDIS_DEFAULT_PORT = 6379;

  private static class EmbeddedRedisSingleton {
    private static final GenericContainer<?> INSTANCE = createEmbeddedRedis();
    private static GenericContainer<?> createEmbeddedRedis() {
      String imageName = Optional.ofNullable(System.getenv(REDIS_IMAGE_ENV_VARIABLE)).orElse(DEFAULT_REDIS_IMAGE);

      GenericContainer<?> container = new GenericContainer<>(imageName)
          .withExposedPorts(REDIS_DEFAULT_PORT);
      container.start();
      return container;
    }
  }

  public static GenericContainer<?> getEmbeddedRedis() {
    return EmbeddedRedisFactory.EmbeddedRedisSingleton.INSTANCE;
  }
}
