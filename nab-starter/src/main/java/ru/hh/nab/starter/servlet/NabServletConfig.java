package ru.hh.nab.starter.servlet;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import javax.servlet.Servlet;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ru.hh.nab.starter.NabApplicationBuilder;

public interface NabServletConfig {

  String[] getMapping();

  String getName();

  default Map<String, String> getInitParameters() {
    return Collections.emptyMap();
  }

  default AnnotationConfigWebApplicationContext createActiveChildCtx(WebApplicationContext rootContext, Class<?>... cfgClasses) {
    AnnotationConfigWebApplicationContext childConfig = new AnnotationConfigWebApplicationContext();
    childConfig.setParent(rootContext);
    childConfig.setServletContext(rootContext.getServletContext());
    if (cfgClasses.length > 0) {
      childConfig.register(cfgClasses);
    }
    childConfig.refresh();
    return childConfig;
  }

  Servlet createServlet(WebApplicationContext rootCtx);

  default boolean isDisabled() {
    return false;
  }

  final class Builder {

    private final NabApplicationBuilder nabApplicationBuilder;
    private final Class<?>[] childConfigurations;
    private final Function<WebApplicationContext, Servlet> servletInitializer;
    private String[] mappings;
    private String servletName;

    public Builder(NabApplicationBuilder nabApplicationBuilder,
                   Function<WebApplicationContext, Servlet> servletInitializer,
                   Class<?>... childConfigurations) {
      this.nabApplicationBuilder = nabApplicationBuilder;
      this.servletInitializer = servletInitializer;
      this.childConfigurations = childConfigurations;
    }

    public Builder setServletName(String servletName) {
      this.servletName = servletName;
      return this;
    }

    public NabApplicationBuilder bindTo(String... mappings) {
      this.mappings = mappings;
      return nabApplicationBuilder.addServlet(this);
    }

    public NabApplicationBuilder bindToRoot() {
      return bindTo(NabApplicationBuilder.ROOT_MAPPING);
    }

    public NabServletConfig build() {
      return new NabServletConfig() {
        @Override
        public String[] getMapping() {
          return mappings;
        }

        @Override
        public String getName() {
          if (StringUtils.isEmpty(servletName)) {
            return String.join("", getMapping());
          }
          return servletName;
        }

        @Override
        public Servlet createServlet(WebApplicationContext rootCtx) {
          WebApplicationContext context = createActiveChildCtx(rootCtx, childConfigurations);
          return servletInitializer.apply(context);
        }
      };
    }

  }
}
