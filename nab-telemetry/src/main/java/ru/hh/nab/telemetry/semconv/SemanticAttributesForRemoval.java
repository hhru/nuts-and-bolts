package ru.hh.nab.telemetry.semconv;

import io.opentelemetry.api.common.AttributeKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import io.opentelemetry.semconv.incubating.CodeIncubatingAttributes;
import io.opentelemetry.semconv.incubating.HttpIncubatingAttributes;

/**
 * TODO HH-278535
 * @deprecated attributes from this class will be removed and only need for smooth transition
 */
@Deprecated(forRemoval = true)
public class SemanticAttributesForRemoval {

  /**
   * @deprecated TODO HH-278535 this attribute will be removed, use CodeAttributes.CODE_FUNCTION_NAME instead
   * Note that CodeAttributes.CODE_FUNCTION_NAME is equal <code>CODE_NAMESPACE + "." + CODE_FUNCTION.</code>
   * You can obtain old CODE_FUNCTION by <code>codeFunctionName.substring(0, codeFunctionName.lastIndexOf('.'))</code>.
   * @see io.opentelemetry.semconv.CodeAttributes#CODE_FUNCTION_NAME
   */
  @Deprecated(forRemoval = true)
  public static final AttributeKey<String> CODE_FUNCTION = CodeIncubatingAttributes.CODE_FUNCTION;

  /**
   * @deprecated TODO HH-278535 this attribute will be removed, use CodeAttributes.CODE_FUNCTION_NAME instead
   * Note that CodeAttributes.CODE_FUNCTION_NAME is equal <code>CODE_NAMESPACE + "." + CODE_FUNCTION.</code>
   * You can obtain old CODE_NAMESPACE by <code>codeFunctionName.substring(codeFunctionName.lastIndexOf('.') + 1)</code>.
   * @see io.opentelemetry.semconv.CodeAttributes#CODE_FUNCTION_NAME
   */
  @Deprecated(forRemoval = true)
  public static final AttributeKey<String> CODE_NAMESPACE = CodeIncubatingAttributes.CODE_NAMESPACE;

  /**
   * @deprecated TODO HH-278535 this attribute will be removed, use ClientAttributes.CLIENT_ADDRESS instead
   * @see io.opentelemetry.semconv.ClientAttributes#CLIENT_ADDRESS
   */
  @Deprecated(forRemoval = true)
  public static final AttributeKey<String> HTTP_CLIENT_IP = HttpIncubatingAttributes.HTTP_CLIENT_IP;

  /**
   * @deprecated TODO HH-278535 this attribute will be removed, use HttpAttributes.HTTP_RESPONSE_STATUS_CODE instead
   * @see io.opentelemetry.semconv.HttpAttributes#HTTP_RESPONSE_STATUS_CODE
   */
  @Deprecated(forRemoval = true)
  public static final AttributeKey<Long> HTTP_STATUS_CODE = HttpIncubatingAttributes.HTTP_STATUS_CODE;

  /**
   * @deprecated TODO HH-278535 this attribute will be removed, use ServerAttributes.SERVER_ADDRESS instead
   * @see io.opentelemetry.semconv.ServerAttributes#SERVER_ADDRESS
   */
  @Deprecated(forRemoval = true)
  public static final AttributeKey<String> HTTP_HOST = HttpIncubatingAttributes.HTTP_HOST;

  /**
   * @deprecated TODO HH-278535 this attribute will be removed, use HttpAttributes.HTTP_REQUEST_METHOD instead
   * @see io.opentelemetry.semconv.HttpAttributes#HTTP_REQUEST_METHOD
   */
  @Deprecated(forRemoval = true)
  public static final AttributeKey<String> HTTP_METHOD = HttpIncubatingAttributes.HTTP_METHOD;

  /**
   * @deprecated TODO HH-278535 this attribute will be removed, use NabPeerAttributes.PEER_CLOUD_AVAILABILITY_ZONE instead
   * @see ru.hh.nab.telemetry.semconv.NabPeerAttributes#PEER_CLOUD_AVAILABILITY_ZONE
   */
  @Deprecated(forRemoval = true)
  public static final AttributeKey<String> HTTP_REQUEST_CLOUD_REGION = stringKey("http.request.cloud.region");

  /**
   * @deprecated TODO HH-278535 this attribute will be removed, use UrlAttributes.URL_SCHEME instead
   * @see io.opentelemetry.semconv.UrlAttributes#URL_SCHEME
   */
  @Deprecated(forRemoval = true)
  public static final AttributeKey<String> HTTP_SCHEME = HttpIncubatingAttributes.HTTP_SCHEME;

  /**
   * @deprecated TODO HH-278535 this attribute will be removed, use UrlAttributes.URL_PATH & UrlAttributes.URL_QUERY instead.
   * Note that HTTP_TARGET is equal <code>UrlAttributes.URL_PATH + (UrlAttributes.URL_QUERY == null ? "" : "?" + UrlAttributes.URL_QUERY)</code>
   * @see io.opentelemetry.semconv.UrlAttributes#URL_PATH
   * @see io.opentelemetry.semconv.UrlAttributes#URL_QUERY
   */
  @Deprecated(forRemoval = true)
  public static final AttributeKey<String> HTTP_TARGET = HttpIncubatingAttributes.HTTP_TARGET;

  /**
   * @deprecated TODO HH-278535 this attribute will be removed, use UrlAttributes.URL_FULL instead
   * @see io.opentelemetry.semconv.UrlAttributes#URL_FULL
   */
  @Deprecated(forRemoval = true)
  public static final AttributeKey<String> HTTP_URL = HttpIncubatingAttributes.HTTP_URL;

  private SemanticAttributesForRemoval() {}
}
