package ru.hh.dxm;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import ru.hh.util.trees.Tree;
import ru.hh.util.trees.TreeNavigator;

public class TreeNavigatorTest {
  private static class SimpleStringTree implements Tree<String, String, SimpleStringTree> {
    private final String payload;
    private final Map<String, SimpleStringTree> ways;

    public SimpleStringTree(String payload) {
      this.payload = payload;
      this.ways = ImmutableMap.of();
    }

    public SimpleStringTree(String payload, Map<String, SimpleStringTree> ways) {
      this.payload = payload;
      this.ways = ways;
    }

    @Override
    public String get() {
      return payload;
    }

    @Override
    public SimpleStringTree sub(String way) {
      return ways.get(way);
    }

    @Override
    public Iterable<SimpleStringTree> subs() {
      return ways.values();
    }
  }

  /*
  /a:A
    /b:B
    /c:C
      /d:D
    /e:E
   */

  @Test
  public void test() {
    SimpleStringTree tree = new SimpleStringTree("A",
            ImmutableMap.<String, SimpleStringTree>of(
                    "b", new SimpleStringTree("B"),
                    "c", new SimpleStringTree("C", ImmutableMap.<String, SimpleStringTree>of(
                            "d", new SimpleStringTree("D"))),
                    "e", new SimpleStringTree("E")));

    TreeNavigator<String, String, SimpleStringTree> nav = TreeNavigator.of(tree);

    Assert.assertEquals("A", nav.tree().get());

    nav.descend("b");
    Assert.assertEquals("B", nav.tree().get());

    nav.ascend();
    Assert.assertEquals("A", nav.tree().get());

    nav.descend("c");
    Assert.assertEquals("C", nav.tree().get());

    nav.descend("d");
    Assert.assertEquals("D", nav.tree().get());

    nav.ascend();
    nav.ascend();

    nav.descend("e");
    Assert.assertEquals("E", nav.tree().get());
  }
}
