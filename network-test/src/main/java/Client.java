import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.commons.cli.Option.builder;

public class Client {

  private static final String PORT_PROP = "port";
  private static final String THREADS_PROP = "threads";
  private static final String CONNECTION_PROP = "connectionTimeoutMs";
  private static final String READ_TIMEOUT_PROP = "readTimeoutMs";
  private static final String REQUEST_INTERVAL_PROP = "requestIntervalMs";
  private static final String STATISTIC_SEND_INTERVAL_PROP = "statIntervalSec";
  private static final String HOST_PROP = "host";

  private static final String SUCCESS = "success";

  public static void main(String[] args) throws InterruptedException {
    Options options = new Options();
    options
      .addOption(builder(PORT_PROP).longOpt(PORT_PROP).type(Integer.class).hasArg(true).desc("target server port").required(true).build())
      .addOption(builder(THREADS_PROP).type(Integer.class).longOpt(THREADS_PROP).hasArg(true).desc("workers count").required(true).build())
      .addOption(builder(CONNECTION_PROP).longOpt(CONNECTION_PROP).hasArg(true).desc("connection timeout ms").required(false).build())
      .addOption(builder(READ_TIMEOUT_PROP).longOpt(READ_TIMEOUT_PROP).hasArg(true).desc("read timeout ms").required(false).build())
      .addOption(builder(REQUEST_INTERVAL_PROP).longOpt(REQUEST_INTERVAL_PROP).hasArg(true).desc("sleep ms after request").required(false).build())
      .addOption(builder(STATISTIC_SEND_INTERVAL_PROP).type(Long.class).longOpt(STATISTIC_SEND_INTERVAL_PROP).hasArg(true).required(false).build())
      .addOption(builder(HOST_PROP).longOpt(HOST_PROP).hasArg(true).desc("application host").required(false).build());
    try {
      var cmd = new DefaultParser().parse(options, args, true);
      ConcurrentMap<String, LongAdder> errorStat = new ConcurrentHashMap<>();
      Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
        long successCount = Optional.ofNullable(errorStat.get("success")).map(LongAdder::sum).orElse(0L);
        System.out.println("Successful requests(should be 0 if everything works as expected): " + successCount);
        errorStat.entrySet().stream().filter(entry -> !SUCCESS.equals(entry.getKey())).forEach(entry ->
          System.out.println("Error message <" + entry.getKey() + ">: " + Optional.ofNullable(entry.getValue()).map(LongAdder::sum).orElse(0L))
        );
      }, 0, Long.parseLong(cmd.getOptionValue(STATISTIC_SEND_INTERVAL_PROP, "5")), TimeUnit.SECONDS);
      int threads = Integer.parseInt(cmd.getOptionValue(THREADS_PROP));
      var workers = IntStream.range(0, threads).mapToObj(threadIndex -> {
        var worker = new Thread(() -> {
          long requestSendIntervalMs = Long.parseLong(cmd.getOptionValue(REQUEST_INTERVAL_PROP, "0"));
          try {
            URL url = new URL("http://" + cmd.getOptionValue(HOST_PROP, "localhost") + ":" + Integer.parseInt(cmd.getOptionValue(PORT_PROP))
              + "/status"
            );
            while (true) {
              try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(Integer.parseInt(cmd.getOptionValue(CONNECTION_PROP, "100")));
                connection.setReadTimeout(Integer.parseInt(cmd.getOptionValue(READ_TIMEOUT_PROP, "1000")));
                OutputStream.nullOutputStream().write(connection.getResponseCode());
                errorStat.computeIfAbsent(SUCCESS, key -> new LongAdder()).increment();
              } catch (IOException e) {
                errorStat.computeIfAbsent(e.getClass().getName() + ":" + e.getMessage(), key -> new LongAdder()).increment();
              }

              if (requestSendIntervalMs != 0) {
                trySleep(requestSendIntervalMs);
              }
            }
          } catch (MalformedURLException e) {
            throw new RuntimeException(e);
          }

        });
        worker.setDaemon(true);
        worker.setName("worker-" + threadIndex);
        worker.start();
        return worker;
      }).collect(Collectors.toList());
      for (Thread worker : workers) {
        worker.join();
      }
    } catch (ParseException pe) {
      System.out.println("Failed to parse arguments. Exiting");
      pe.printStackTrace();
      System.exit(1);
    }
  }

  private static void trySleep(long requestSendIntervalMs) {
    try {
      Thread.sleep(requestSendIntervalMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
