package ru.hh.nab.core.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.MBeanExporterListener;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class HttpAdaptorManagerMBean implements MBeanExporterListener {
  private static final Logger logger = LoggerFactory.getLogger(HttpAdaptorManagerMBean.class);
  static final String ADAPTOR_NAME = "Server:name=HttpAdaptor";

  private final MBeanServer mbeanServer;

  public HttpAdaptorManagerMBean(MBeanServer mbeanServer) {
    this.mbeanServer = mbeanServer;
  }

  public void mbeanRegistered(ObjectName objectName) {
    if (ADAPTOR_NAME.equals(objectName.getCanonicalName())) {
      try {
        mbeanServer.invoke(objectName, "start", null, null);
      } catch (Exception e) {
        logger.error("Can't start HttpAdaptor: " + e);
      }
    }
  }

  public void mbeanUnregistered(ObjectName objectName) {
    if (ADAPTOR_NAME.equals(objectName.getCanonicalName())) {
      try {
        mbeanServer.invoke(objectName, "stop", null, null);
      } catch (Exception e) {
        logger.error("Can't stop HttpAdaptor: " + e);
      }
    }
  }
}
