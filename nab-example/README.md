### Running in IntelliJ IDEA:

* Main class: `ru.hh.nab.example.ExampleMain`
* VM parameters: `-DsettingsDir=src/etc/nab-spring-example`
* Working directory: $MODULE_DIR$
* Use classpath of module: nab-spring-example

### Running with maven-exec-plugin:
```
cd nab-spring-example
mvn exec:java
```

The output in console should look like:

```
[2018-01-24 15:57:39,396] [main] INFO  org.eclipse.jetty.util.log rid: - Logging initialized @1479ms to org.eclipse.jetty.util.log.Slf4jLog
[2018-01-24 15:57:39,778] [main] INFO  org.eclipse.jetty.server.Server rid: - jetty-9.4.6.v20170531
[2018-01-24 15:57:40,289] [main] INFO  o.e.j.server.handler.ContextHandler rid: - Started o.e.j.s.ServletContextHandler@59cba5a{/,null,AVAILABLE}
[2018-01-24 15:57:40,306] [main] INFO  o.e.jetty.server.AbstractConnector rid: - Started HHServerConnector@d278d2b{HTTP/1.1,[http/1.1]}{0.0.0.0:9999}
[2018-01-24 15:57:40,307] [main] INFO  org.eclipse.jetty.server.Server rid: - Started @2392ms
example (ver. ${project.version}) started at 2018-01-24T15:57:39.499, pid 12991, listening to port 9999
```

### Example resource:

```
curl http://localhost:9999/hello
```

should return:

```
Hello, world!
```
