package ru.hh.nab.starter.server.filter;

import java.io.Serializable;

class Header implements Serializable {
  private static final long serialVersionUID = -1198547959156704720L;

  public final String header;
  public final String value;

  Header(String header, String value) {
    this.header = header;
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Header header1 = (Header) o;

    if (!header.equals(header1.header)) {
      return false;
    }

    return value.equals(header1.value);
  }

  @Override
  public int hashCode() {
    int result = header.hashCode();
    result = 31 * result + value.hashCode();
    return result;
  }
}

