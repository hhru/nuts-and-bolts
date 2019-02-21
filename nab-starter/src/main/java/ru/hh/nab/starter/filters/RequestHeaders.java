package ru.hh.nab.starter.filters;

public interface RequestHeaders {
  String REQUEST_ID = "x-request-id";
  String REQUEST_ID_DEFAULT = "NoRequestId";
  String REQUEST_SOURCE = "x-source";
  String LOAD_TESTING = "x-load-testing";
}
