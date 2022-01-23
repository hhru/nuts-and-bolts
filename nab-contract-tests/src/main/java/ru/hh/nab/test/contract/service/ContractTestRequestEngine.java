package ru.hh.nab.test.contract.service;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.asynchttpclient.netty.NettyResponse;
import org.asynchttpclient.netty.NettyResponseStatus;
import org.asynchttpclient.uri.Uri;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestEngine;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResponseConverterUtils;
import ru.hh.nab.test.contract.model.ExpectedResponse;

public class ContractTestRequestEngine implements RequestEngine {

  private final Request request;

  public ContractTestRequestEngine(Request request) {
    this.request = request;
  }

  @Override
  public CompletableFuture<Response> execute() {
    ExpectedResponse expectedResponse = ExpectationsStorage.add(request);
    return CompletableFuture.completedFuture(buildResponse(expectedResponse));
  }

  private Response buildResponse(ExpectedResponse expectedResponse) {
    Uri requestUri = Uri.create(request.getUrl());

    DefaultHttpHeaders headers = new DefaultHttpHeaders(false);
    if (expectedResponse.getHeaders() != null) {
      expectedResponse.getHeaders().forEach(headers::add);
    }

    DefaultHttpResponse httpResponse = new DefaultHttpResponse(
        HttpVersion.HTTP_1_1,
        HttpResponseStatus.valueOf(expectedResponse.getStatus()),
        headers
    );
    NettyResponseStatus nettyResponseStatus = new NettyResponseStatus(requestUri, httpResponse, null);

    NettyResponse nettyResponse = new NettyResponse(
        nettyResponseStatus,
        headers,
        List.of()
    );

    return ResponseConverterUtils.convert(nettyResponse);
  }
}
