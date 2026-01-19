package ru.hh.nab.telemetry.semconv;

import io.opentelemetry.api.common.AttributeKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

public class NabPeerAttributes {

  public static final AttributeKey<String> PEER_CLOUD_AVAILABILITY_ZONE = stringKey("peer.cloud.availability_zone");

  private NabPeerAttributes() {}
}
