package ru.hh.nab.common.constants;

public class RequestAttributes {

  /**
   * The method fully-qualified name without arguments.
   * Examples:
   * <ul>
   *   <li>Method: {@code com.example.MyHttpService.serveRequest}
   *   <li>Anonymous class method: {@code com.mycompany.Main$1.myMethod}
   *   <li>Lambda method: {@code com.mycompany.Main$$Lambda/0x0000748ae4149c00.myMethod}
   * </ul>
   */
  public static final String CODE_FUNCTION_NAME = "codeFunctionName";
  public static final String HTTP_ROUTE = "httpRoute";

  private RequestAttributes() {}
}
