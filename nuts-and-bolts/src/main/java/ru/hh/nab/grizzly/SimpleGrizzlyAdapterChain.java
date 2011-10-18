package ru.hh.nab.grizzly;

import com.google.common.collect.Lists;
import com.sun.grizzly.tcp.Request;
import com.sun.grizzly.tcp.http11.GrizzlyAdapter;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.grizzly.tcp.http11.GrizzlyResponse;
import java.util.List;
import ru.hh.nab.scopes.RequestScope;

public class SimpleGrizzlyAdapterChain extends GrizzlyAdapter {
  private final static String INVOKED_ADAPTER_FLAG = SimpleGrizzlyAdapterChain.class.getName() + ".invokedAdapter";
  // XXX: Achtung!
  private final static int ADAPTER_ABSTAINED_NOTE = 29;

  public static void abstain(Request r) {
    r.setNote(ADAPTER_ABSTAINED_NOTE, true);
  }

  public static void abstain(GrizzlyRequest r) {
    abstain(r.getRequest());
  }

  private final List<GrizzlyAdapter> adapters = Lists.newArrayList();

  public void addGrizzlyAdapter(GrizzlyAdapter adapter) {
    adapters.add(adapter);
  }

  @Override
  public void service(GrizzlyRequest request, GrizzlyResponse response) throws Exception {
    RequestScope.enter(request);
    try {
      for (GrizzlyAdapter adapter : adapters) {
        try {
          request.setNote(INVOKED_ADAPTER_FLAG, adapter);
          adapter.service(request.getRequest(), response.getResponse());
          if (request.getRequest().getNote(ADAPTER_ABSTAINED_NOTE) == null)
            return;
        } finally {
          request.getRequest().setNote(ADAPTER_ABSTAINED_NOTE, null);
        }
      }
    } finally {
      RequestScope.leave();
    }
    response.sendError(404, "No handler found");
  }

  @Override
  public void afterService(GrizzlyRequest request, GrizzlyResponse response) throws Exception {
    GrizzlyAdapter invokedAdapter = (GrizzlyAdapter) request.getNote(INVOKED_ADAPTER_FLAG);
    for (GrizzlyAdapter a : adapters) {
      try {
        a.afterService(request, response);
      } finally {
        request.removeNote(INVOKED_ADAPTER_FLAG);
      }
      if (a == invokedAdapter)
        return;
    }
  }
}
