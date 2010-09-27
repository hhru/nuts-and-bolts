package ru.hh.util.trees;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Test;

public class ExternalTreeBuilderTest {
  @Test
  public void emptyTree() {
    ExternalTreeBuilder<String, String> builder = ExternalTreeBuilder.of("~", String.class);

    ExternalTree<String, String> tree = builder.build();

    Assert.assertEquals("~", tree.get());
    Assert.assertTrue(Iterables.isEmpty(tree.subs()));
  }

  @Test
  public void oneLevelTree() {
    ExternalTreeBuilder<String, String> builder = ExternalTreeBuilder.of("~", String.class);
    builder.add(new String[]{"a"}, "A");
    builder.add(new String[]{"b"}, "B");
    ExternalTree<String, String> tree = builder.build();

    Assert.assertEquals("~", tree.get());
    Assert.assertEquals("A", tree.sub("a").get());
    Assert.assertEquals("B", tree.sub("b").get());
  }

  @Test
  public void multiLevelTree() {
    ExternalTreeBuilder<String, String> builder = ExternalTreeBuilder.of("~", String.class);
    builder.add(new String[]{"a", "b"}, "AB");
    ExternalTree<String, String> tree = builder.build();

    Assert.assertEquals("~", tree.get());
    Assert.assertEquals("~", tree.sub("a").get());
    Assert.assertEquals("AB", tree.sub("a").sub("b").get());
  }
}
