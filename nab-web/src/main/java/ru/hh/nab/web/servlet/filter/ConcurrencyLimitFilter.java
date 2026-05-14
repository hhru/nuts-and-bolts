package ru.hh.nab.web.servlet.filter;

import com.netflix.concurrency.limits.Limit;
import com.netflix.concurrency.limits.Limiter;
import com.netflix.concurrency.limits.limit.Gradient2Limit;
import com.netflix.concurrency.limits.limiter.SimpleLimiter;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import ru.hh.nab.web.http.HttpStatus;

public class ConcurrencyLimitFilter implements Filter {
  private final Limiter<HttpServletRequest> limiter;

  private static final Set<Integer> STATUSES_TO_DROP_CONCURRENCY = Set.of(
      HttpStatus.SERVER_TIMEOUT.getStatusCode(),
      HttpStatus.SERVICE_PARTIALLY_UNAVAILABLE.getStatusCode(),
      HttpServletResponse.SC_SERVICE_UNAVAILABLE
  );

  public ConcurrencyLimitFilter(int initialLimit, int minLimit, int maxConcurrency, int queueSize) {
    var gradientLimit = Gradient2Limit.newBuilder()
        .initialLimit(initialLimit)
        .minLimit(minLimit)
        .maxConcurrency(maxConcurrency)
        .queueSize(queueSize)
        .build();
    this.limiter = SimpleLimiter.newBuilder()
        .limit(gradientLimit)
        .build();
  }

  public ConcurrencyLimitFilter(Limit limit) {
    this.limiter = SimpleLimiter.newBuilder()
        .limit(limit)
        .build();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    var httpRequest = (HttpServletRequest) request;
    var httpResponse = (HttpServletResponse) response;

    var limiterListener = limiter.acquire(httpRequest).orElse(null);
    if (limiterListener == null) {
      httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      return;
    }

    try {
      chain.doFilter(request, response);
    } finally {
      if (request.isAsyncStarted()) {
        var asyncContext = request.getAsyncContext();
        asyncContext.addListener(new AsyncListener() {
          @Override
          public void onComplete(AsyncEvent asyncEvent) {
            var httpServletResponse = (HttpServletResponse) asyncEvent.getSuppliedResponse();
            applyListener(httpServletResponse, limiterListener);
          }

          @Override
          public void onTimeout(AsyncEvent asyncEvent) {
            limiterListener.onDropped();
          }

          @Override
          public void onError(AsyncEvent asyncEvent) {
            var httpServletResponse = (HttpServletResponse) asyncEvent.getSuppliedResponse();
            applyListener(httpServletResponse, limiterListener);
          }

          @Override
          public void onStartAsync(AsyncEvent asyncEvent) {
          }
        });
      } else {
        // Synchronous request - release token now
        applyListener(httpResponse, limiterListener);
      }
    }
  }

  private static void applyListener(HttpServletResponse httpServletResponse, Limiter.Listener listener) {
    if (httpServletResponse.getStatus() < HttpServletResponse.SC_BAD_REQUEST) {
      listener.onSuccess();
    } else if (STATUSES_TO_DROP_CONCURRENCY.contains(httpServletResponse.getStatus())) {
      listener.onDropped();
    } else {
      // ignore 4xx in order to exclude their RTT from statistics, because it could be significantly lower comparing with success requests.
      listener.onIgnore();
    }
  }
}
