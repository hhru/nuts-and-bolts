import com.sun.net.httpserver.HttpServer;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.starter.NabProdConfig;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public class Server {
  public static void main(String[] args) throws IOException {
    Options options = new Options();
    options.addOption(Option.builder("port").type(Integer.class).longOpt("port").hasArg(true).desc("application port").required(true).build());
    try {
      var cmd = new DefaultParser().parse(options, args, true);
      try (var outputStream = new FileOutputStream("service.properties")) {
        Properties properties = new Properties();
        properties.setProperty("log.toConsole", "true");
        properties.setProperty("serviceName", "network-test-server");
        properties.setProperty("datacenter", "test");
        properties.setProperty("jetty.port", cmd.getOptionValue("port"));
        properties.store(outputStream, "");
      }
      int consulPort = runConsulMock();
      System.out.println("Consul mock run on port: " + consulPort);
      System.setProperty(NabProdConfig.CONSUL_PORT_ENV_KEY, String.valueOf(consulPort));
      NabApplication.builder().build().run(ServerConfig.class);
    } catch (ParseException pe) {
      System.out.println("Failed to parse arguments. Exiting");
      System.exit(1);
    }

  }

  private static int runConsulMock() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 1);
    server.createContext("/v1/agent/self", exchange -> exchange.sendResponseHeaders(200, 0));
    server.createContext("/v1/agent/service/register", exchange -> exchange.sendResponseHeaders(200, 0));
    server.setExecutor(null);
    server.start();
    return server.getAddress().getPort();
  }
}
