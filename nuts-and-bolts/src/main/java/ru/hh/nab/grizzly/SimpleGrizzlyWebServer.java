package ru.hh.nab.grizzly;

import com.sun.grizzly.SSLConfig;
import com.sun.grizzly.arp.AsyncFilter;
import com.sun.grizzly.arp.AsyncHandler;
import com.sun.grizzly.arp.DefaultAsyncHandler;
import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.ssl.SSLSelectorThread;
import com.sun.grizzly.tcp.http11.GrizzlyAdapter;
import com.sun.grizzly.tcp.http11.GrizzlyListener;
import com.sun.grizzly.util.net.jsse.JSSEImplementation;
import java.io.IOException;
import java.util.ArrayList;

public class SimpleGrizzlyWebServer {
  private static final int DEFAULT_PORT = 8080;

  // The underlying {@link GrizzlyListener}
  private GrizzlyListener grizzlyListener;

  private SimpleGrizzlyAdapterChain adapterChains = new SimpleGrizzlyAdapterChain();

  private boolean isStarted = false;

  private ArrayList<AsyncFilter> asyncFilters = new ArrayList<AsyncFilter>();

  public SimpleGrizzlyWebServer() {
    this(DEFAULT_PORT, 1);
  }

  public SimpleGrizzlyWebServer(int port) {
    this(port, 1);
  }

  public SimpleGrizzlyWebServer(int port, int maxThreads) {
    this(port, maxThreads, false);
  }

  public SimpleGrizzlyWebServer(int port, boolean secure) {
    this(port, 1, secure);
  }

  public SimpleGrizzlyWebServer(int port, int maxThreads, boolean secure) {
    createSelectorThread(port, secure);
    setMaxThreads(maxThreads);
  }

  private void createSelectorThread(int port, boolean secure) {
    if (secure) {
      SSLSelectorThread sslSelectorThread = new SSLSelectorThread();
      try {
        sslSelectorThread.setSSLImplementation(new JSSEImplementation());
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(e);
      }
      grizzlyListener = sslSelectorThread;
    } else {
      grizzlyListener = new SelectorThread();
    }
    ((SelectorThread) grizzlyListener).setPort(port);
  }

  public SelectorThread getSelectorThread() {
    return (SelectorThread) grizzlyListener;
  }

  public void addAsyncFilter(AsyncFilter asyncFilter) {
    asyncFilters.add(asyncFilter);
  }

  public void addGrizzlyAdapter(GrizzlyAdapter grizzlyAdapter) {
    adapterChains.addGrizzlyAdapter(grizzlyAdapter);
  }

  public void setSSLConfig(SSLConfig sslConfig) {
    if (!(grizzlyListener instanceof SSLSelectorThread)) {
      throw new IllegalStateException("This instance isn't supporting SSL/HTTPS");
    }
    ((SSLSelectorThread) grizzlyListener).setSSLConfig(sslConfig);
  }

  public void useAsynchronousWrite(boolean asyncWrite) {
    if (!(grizzlyListener instanceof SelectorThread)) {
      ((SelectorThread) grizzlyListener).setAsyncHttpWriteEnabled(asyncWrite);
    }
  }

  public void start() throws IOException {
    if (isStarted) return;
    if (grizzlyListener instanceof SSLSelectorThread) {
      // if needed create default SSLContext
      SSLSelectorThread sslST = (SSLSelectorThread) grizzlyListener;
      if (sslST.getSSLContext() == null) {
        SSLConfig sslConfig = new SSLConfig(true);
        if (!sslConfig.validateConfiguration(true)) {
          // failed to create default
          throw new RuntimeException("Failed to create default SSLConfig.");
        }
        sslST.setSSLContext(sslConfig.createSSLContext());
      }
    }
    isStarted = true;

    adapterChains.setHandleStaticResources(false);
    grizzlyListener.setAdapter(adapterChains);

    if (asyncFilters.size() > 0) {
      ((SelectorThread) grizzlyListener).setEnableAsyncExecution(true);
      AsyncHandler asyncHandler = new DefaultAsyncHandler();
      for (AsyncFilter asyncFilter : asyncFilters) {
        asyncHandler.addAsyncFilter(asyncFilter);
      }
      ((SelectorThread) grizzlyListener).setAsyncHandler(asyncHandler);
    }

    try {
      grizzlyListener.listen();
    } catch (InstantiationException ex) {
      throw new IOException(ex.getMessage());
    }
  }

  public void setCoreThreads(int coreThreads) {
    ((SelectorThread) grizzlyListener).setCoreThreads(coreThreads);
  }

  public void setMaxThreads(int maxThreads) {
    ((SelectorThread) grizzlyListener).setMaxThreads(maxThreads);
  }

  public void stop() {
    if (!isStarted) return;
    isStarted = false;
    if (grizzlyListener instanceof SelectorThread) {
      ((SelectorThread) grizzlyListener).stopEndpoint();
    }
  }
}
