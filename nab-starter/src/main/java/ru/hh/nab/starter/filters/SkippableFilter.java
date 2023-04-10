package ru.hh.nab.starter.filters;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

public abstract class SkippableFilter extends OncePerRequestFilter {
  private static final UrlPathHelper urlPathHelper = new UrlPathHelper();
  private String exclusionsString;

  protected final void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = getCurrentPath(request);

    if (getSplittedPaths().anyMatch(path::equals)) {
      filterChain.doFilter(request, response);
      return;
    }

    performFilter(request, response, filterChain);
  }

  protected abstract void performFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException;

  protected String getCurrentPath(HttpServletRequest request) {
    return StringUtils.substringBefore(getUrlPathHelper().getLookupPathForRequest(request), ".");
  }

  protected Stream<String> getSplittedPaths() {
    if (StringUtils.isEmpty(exclusionsString)) {
      return Stream.empty();
    }

    return Pattern.compile(",").splitAsStream(exclusionsString.replace("\n", ""))
        .filter(StringUtils::isNotBlank)
        .map(String::trim);
  }

  protected UrlPathHelper getUrlPathHelper() {
    return urlPathHelper;
  }

  public void setExclusionsString(String exclusionsString) {
    this.exclusionsString = exclusionsString;
  }
}
