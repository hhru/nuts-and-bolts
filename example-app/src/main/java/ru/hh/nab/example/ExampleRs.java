package ru.hh.nab.example;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import com.google.common.base.Function;
import ru.hh.nab.async.Async;
import ru.hh.nab.async.Callback;
import ru.hh.nab.async.TimedAsync;
import ru.hh.nab.health.monitoring.TimingsLogger;
import java.io.IOException;

@Path("/")
@Singleton
public class ExampleRs {
  @Inject
  private Provider<TimingsLogger> loggerProvider;

  @GET
  @Path("/hello")
  public String hello(@DefaultValue("world")
      @QueryParam("name")
      String name) {
    loggerProvider.get().probe("hello.entry-point");
    try {
      return String.format("Hello, %s!", name);
    } finally {
      loggerProvider.get().probe("hello.exit-point");
    }
  }

  @GET
  @Path("/weird/timed/hello")
  public String helloTimedAsync(final @DefaultValue("world")
                      @QueryParam("name")
                      String name) {
    loggerProvider.get().probe("hello.entry-point");
    try {
      final StringBuilder response = new StringBuilder();
      TimedAsync.startTimedAsync()
          .thenTimed(
              "first step must finish ok",
              new Function<Void, Async<String>>() {
                @Override
                public Async<String> apply(Void from) {
                  return new Async<String>() {
                    @Override
                    protected void runExposed(Callback<String> onSuccess, Callback<Throwable> onError) throws Exception {
                      onSuccess.call("all is good");
                    }
                  };
                }
              })
          .thenTimed(
              "second step must fail",
              new Function<String, Async<Integer>>() {
                @Override
                public Async<Integer> apply(String string) {
                  throw new RuntimeException();
                }
              })
          .thenTimed(
              "should not see this in logs because onError is called on previous step",
              new Function<Integer, Async<Void>>() {
                @Override
                public Async<Void> apply(Integer profile) {
                  return new Async<Void>() {
                    @Override
                    protected void runExposed(Callback<Void> onSuccess, Callback<Throwable> onError) throws Exception {
                    }
                  };
                }
              })
          .run(
              new Callback<Void>() {
                @Override
                public void call(Void from) throws IOException {
                  response.append(String.format("Hello, %s! Something is wrong", name));
                }
              },
              new Callback<Throwable>() {
                @Override
                public void call(Throwable t) throws IOException {
                  response.append(String.format("Hello, %s! All is ok", name));
                }
              }
          );
      return response.toString();
    } finally {
      loggerProvider.get().probe("hello.exit-point");
    }
  }
}
