package ru.hh.util.trees;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;

public final class ExternalTreeBuilder<COLOR, PAYLOAD> {
  private static class TreeHolder<COLOR, PAYLOAD> {
    private final PAYLOAD payload;
    private final Map<COLOR, TreeHolder<COLOR, PAYLOAD>> branches = Maps.newHashMap();

    public TreeHolder(PAYLOAD payload) {
      this.payload = payload;
    }

    public void sub(COLOR color, PAYLOAD payload) {
      if (branches.containsKey(color)) {
        throw new IllegalStateException("key already present " + color);
      }
      branches.put(color, new TreeHolder<COLOR, PAYLOAD>(payload));
    }

    public TreeHolder<COLOR, PAYLOAD> trySub(COLOR color, PAYLOAD payload) {
      TreeHolder<COLOR, PAYLOAD> sub = branches.get(color);
      if (sub == null) {
        sub = new TreeHolder<COLOR, PAYLOAD>(payload);
        branches.put(color, sub);
      }
      return sub;
    }

    public ExternalTree<COLOR, PAYLOAD> build() {
      return new ExternalTree<COLOR, PAYLOAD>(payload,
              Maps.transformValues(branches, new Function<TreeHolder<COLOR, PAYLOAD>, ExternalTree<COLOR, PAYLOAD>>() {
                @Override
                public ExternalTree<COLOR, PAYLOAD> apply(@Nullable TreeHolder<COLOR, PAYLOAD> from) {
                  return from.build();
                }
              }));
    }
  }

  private final TreeHolder<COLOR, PAYLOAD> tree;
  private final PAYLOAD intermediatePayload;

  public ExternalTreeBuilder(PAYLOAD intermediatePayload) {
    this.intermediatePayload = intermediatePayload;
    this.tree = new TreeHolder<COLOR, PAYLOAD>(intermediatePayload);
  }

  public static <COLOR, PAYLOAD, P extends PAYLOAD> ExternalTreeBuilder<COLOR, PAYLOAD> of(P intermediatePayload,
                                                                                           Class<COLOR> color) {
    return new ExternalTreeBuilder<COLOR, PAYLOAD>(intermediatePayload);
  }

  public <P extends PAYLOAD> void add(COLOR[] path, P payload) {
    TreeHolder<COLOR, PAYLOAD> current = tree;
    int i;
    for (i = 0; i < path.length - 1; i++) {
      current = current.trySub(path[i], intermediatePayload);
    }
    current.sub(path[i], payload);
  }

  public ExternalTree<COLOR, PAYLOAD> build() {
    return tree.build();
  }
}
