package ru.hh.nab.metrics;

import java.util.Arrays;
import java.util.Comparator;

class MultiTags extends Tags {

  private final Tag[] tags;

  MultiTags(Tag[] tags) {
    Arrays.sort(tags, Comparator.comparing(tag -> tag.name));
    this.tags = tags;
  }

  @Override
  Tag[] getTags() {
    return tags;
  }

  @Override
  public boolean equals(Object thatObject) {
    if (this == thatObject) {
      return true;
    }
    if (thatObject == null || getClass() != thatObject.getClass()) {
      return false;
    }

    MultiTags thatTags = (MultiTags) thatObject;

    return Arrays.equals(tags, thatTags.tags);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(tags);
  }

  @Override
  public String toString() {
    return Arrays.toString(tags);
  }
}
