package ru.hh.util;

import org.apache.commons.lang.StringUtils;

public final class AcceptHeaderFixer {

  // need to remove wap profile parameter which is sent by some mobile browsers, otherwise jersey parser
  // says the header format is wrong. That header parser is right, and mobile browsers are wrong.
  // Such bad content-type usually looks like this:
  //
  //     application/xhtml+xml;profile='http://www.wapforum.org/xhtml'
  //
  // but quotes around url parameter could be different of missing, so it is safer to use just url for detection.
  private static final String WAPFORUM_ACCEPT_PROFILE_PARAMETER = "http://www.wapforum.org/";

  private AcceptHeaderFixer() {}

  // returns null if empty argument or no need to fix header
  public static String fixedAcceptHeaderOrNull(String acceptHeader) {
    return fixSpacesBetweenContentTypesWithoutComma(fixWapProfile(acceptHeader));
  }

  private static String fixWapProfile(String acceptHeader) {
    if (StringUtils.isBlank(acceptHeader)) {
      return null;
    }

    while (StringUtils.isNotEmpty(acceptHeader) && acceptHeader.contains(WAPFORUM_ACCEPT_PROFILE_PARAMETER)) {
      // Cleanup method: 1. find content-type with parameter containing 'http://www.wapforum.org/' substring
      //                 2. keep all content-types before and after it
      //                 3. repeat until there are no such bad content-types (should not be more than one iteration but.)
      //
      StringBuilder fixedAcceptHeader = new StringBuilder();
      final int wapForumUrlPos = acceptHeader.indexOf(WAPFORUM_ACCEPT_PROFILE_PARAMETER);
      // find where bad content-type starts (if it is not the first content-type in header)
      final int lastCommaBefore = acceptHeader.lastIndexOf(',', wapForumUrlPos);
      // find where bad content-type ends (if it is not the last content-type in header)
      final int firstCommaAfter = acceptHeader.indexOf(',', wapForumUrlPos + WAPFORUM_ACCEPT_PROFILE_PARAMETER.length());

      // find and save good beginning of header if present
      if (lastCommaBefore > 0) {
        fixedAcceptHeader.append(acceptHeader.substring(0, lastCommaBefore));
      }
      // add comma separator if needed
      if (lastCommaBefore > 0 && firstCommaAfter > 0) {
        fixedAcceptHeader.append(',');
      }
      // find and save good ending of header if present
      if (firstCommaAfter > 0) {
        fixedAcceptHeader.append(acceptHeader.substring(firstCommaAfter + 1));
      }
      acceptHeader = fixedAcceptHeader.toString();
    }
    return acceptHeader;
  }

  // replace "application/vnd.wap.wmlscriptc, application/vnd.wap.wmlc application/vnd.wap.sic"
  // with "application/vnd.wap.wmlscriptc, application/vnd.wap.wmlc, application/vnd.wap.sic"
  private static String fixSpacesBetweenContentTypesWithoutComma(String acceptHeader) {
    if (StringUtils.isBlank(acceptHeader)) {
      return null;
    }
    // if header starts with a space (first pos == 0). it is bad enough to give up trying to fix it.
    for (int pos = acceptHeader.indexOf(' '); pos > 0; pos = acceptHeader.indexOf(' ', pos + 1)) {
      char characterBeforeSpace = acceptHeader.charAt(pos - 1);
      if (Character.isAlphabetic(characterBeforeSpace) || Character.isDigit(characterBeforeSpace)) {
        acceptHeader = acceptHeader.substring(0, pos - 1) + ',' + acceptHeader.substring(pos);
        pos++;
      }
    }
    return acceptHeader;
  }
}
