package ru.hh.nab.metrics;

class TestUtils {
  static Tags tagsOf(Tag... tags) {
    return new MultiTags(tags);
  }

  private TestUtils(){}
}
