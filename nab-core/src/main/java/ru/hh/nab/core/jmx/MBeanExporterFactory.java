package ru.hh.nab.core.jmx;

import mx4j.tools.adaptor.http.HttpAdaptor;
import org.springframework.jmx.export.MBeanExporter;
import ru.hh.nab.core.util.FileSettings;

import javax.management.MBeanServer;

import static java.util.Collections.singletonMap;

public class MBeanExporterFactory {

  public static MBeanExporter create(FileSettings settings, MBeanServer mBeanServer) {
    HttpAdaptorManagerMBean httpAdaptorManagerMBean = new HttpAdaptorManagerMBean(mBeanServer);

    MBeanExporter exporter = new MBeanExporter();
    exporter.setListeners(httpAdaptorManagerMBean);
    configureHttpAdaptor(settings, exporter);
    return exporter;
  }

  private static void configureHttpAdaptor(FileSettings settings, MBeanExporter exporter) {
    FileSettings mx4jSettings = settings.getSubSettings("mx4j");
    if ("true".equals(mx4jSettings.getString("mode"))) {
      HttpAdaptor httpAdaptor = new HttpAdaptor();
      httpAdaptor.setPort(mx4jSettings.getInteger("port"));
      httpAdaptor.setHost(mx4jSettings.getString("host"));
      exporter.setBeans(singletonMap(HttpAdaptorManagerMBean.ADAPTOR_NAME, httpAdaptor));
    }
  }
}
