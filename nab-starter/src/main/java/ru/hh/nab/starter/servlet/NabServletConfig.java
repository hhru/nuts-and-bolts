package ru.hh.nab.starter.servlet;

import javax.servlet.Servlet;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public interface NabServletConfig {

  String[] getMapping();

  String getName();

  default AnnotationConfigWebApplicationContext createActiveChildCtx(WebApplicationContext rootCtx, Class<?>... cfgClasses) {
    AnnotationConfigWebApplicationContext childConfig = new AnnotationConfigWebApplicationContext();
    childConfig.setParent(rootCtx);
    childConfig.setServletContext(rootCtx.getServletContext());
    childConfig.register(cfgClasses);
    childConfig.refresh();
    return childConfig;
  }

  Servlet createServlet(WebApplicationContext rootCtx);
}
