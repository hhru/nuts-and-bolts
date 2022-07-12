package ru.hh.nab.metrics;

import java.util.Objects;

/**
 * Just a name-value pair that represents a breakdown of a metric.<br/>
 * For example, url=/vacancy, node=192.168.1.1, db_name=master.
 */
public class Tag extends Tags implements Comparable<Tag> {

  public static final String APP_TAG_NAME = "app";
  public static final String DATASOURCE_TAG_NAME = "datasource";

  public final String name;
  public final String value;
  private final String tag;

  public Tag(String name, String value) {
    this.name = name;
    this.value = value;
    this.tag = name + ": " + value;
  }

  @Override
  public String toString() {
    return tag;
  }

  @Override
  public int compareTo(Tag otherTag) {
    return this.tag.compareTo(otherTag.tag);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Tag otherTag = (Tag) o;
    return tag.equals(otherTag.tag);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(tag);
  }

  @Override
  Tag[] getTags() {
    return new Tag[]{this};
  }
}
