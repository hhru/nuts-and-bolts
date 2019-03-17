package ru.hh.nab.metrics;

/**
 * Just a name-value pair that represents a breakdown of a metric.<br/>
 * For example, url=/vacancy, node=192.168.1.1, db_name=master.
 */
public class Tag extends Tags implements Comparable<Tag> {
  public final String name;
  public final String value;

  public Tag(String name, String value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public String toString() {
    return name + ": " + value;
  }

  @Override
  public int compareTo(Tag otherTag) {
    return this.name.compareTo(otherTag.name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Tag tag = (Tag) o;
    return name.equals(tag.name) && value.equals(tag.value);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + value.hashCode();
    return result;
  }

  @Override
  Tag[] getTags() {
    return new Tag[]{this};
  }
}
