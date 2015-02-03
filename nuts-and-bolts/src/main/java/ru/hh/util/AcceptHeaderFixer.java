package ru.hh.util;

import org.apache.commons.lang.StringUtils;

public final class AcceptHeaderFixer {

  // need to remove wap profile parameter which is sent by some mobile browsers, otherwise jersey parser
  // says the header format is wrong. That header parser is right, and mobile browsers are wrong.
  private static final String WAPFORUM_ACCEPT_PROFILE_PARAMETER = "http://www.wapforum.org/";

  private AcceptHeaderFixer() {}

  // returns null if empty argument or no need to fix header
  public static String fixedAcceptHeaderOrNull(String acceptHeader) {
    if (StringUtils.isBlank(acceptHeader)) {
      return null;
    }
    while (StringUtils.isNotEmpty(acceptHeader) && acceptHeader.contains(WAPFORUM_ACCEPT_PROFILE_PARAMETER)) {
      // Expected abomination examples:
      //
      // ;profile='http://www.wapforum.org/xhtml',
      // ;profile="http://www.wapforum.org/xhtml",
      // ;profile=http://www.wapforum.org/xhtml,
      // ;profile='http://www.wapforum.org/xhtml';
      // ;profile="http://www.wapforum.org/xhtml";
      // ;profile=http://www.wapforum.org/xhtml;
      //
      // Cleanup method: 1. remove part starting from ';profile' until http://www.wapforum.org/,
      //                    including the first ';' character;
      //                 2. continue removing character until until the first ',' or ';' character; keep that character.
      //
      StringBuilder fixedAcceptHeader = new StringBuilder();
      int messPos = acceptHeader.indexOf(WAPFORUM_ACCEPT_PROFILE_PARAMETER);
      // find where ';profile=' starts
      int lastSemicolonBeforeMessPos = acceptHeader.lastIndexOf(';', messPos);
      // find and save good beginning of header
      // (lastSemicolonBeforeMessPos should always be > 0 but we check anyway)
      if (lastSemicolonBeforeMessPos > 0) {
        fixedAcceptHeader.append(acceptHeader.substring(0, lastSemicolonBeforeMessPos));
      }
      // find and save good ending of header if present
      for (int charPos = messPos + WAPFORUM_ACCEPT_PROFILE_PARAMETER.length(); acceptHeader.length() > charPos; charPos++) {
        char ch = acceptHeader.charAt(charPos);
        if (ch == ',' || ch == ';') {
          fixedAcceptHeader.append(acceptHeader.substring(charPos));
          break;
        }
      }
      acceptHeader = fixedAcceptHeader.toString();
    }
    return acceptHeader;
  }
}
