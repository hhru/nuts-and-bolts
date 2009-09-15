package ru.hh.util.trees;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public final class TreeNavigator<COLOR, PAYLOAD, T extends Tree<COLOR, PAYLOAD, T>> {
  private final Deque<T> stack;

  public TreeNavigator(T root) {
    this.stack = new ArrayDeque<T>();
    stack.push(root);
  }

  public static <COLOR, PAYLOAD, T extends Tree<COLOR, PAYLOAD, T>> TreeNavigator<COLOR, PAYLOAD, T> of(T tree) {
    return new TreeNavigator<COLOR, PAYLOAD, T>(tree);
  }

  public TreeNavigator<COLOR, PAYLOAD, T> descend(COLOR way) {
    if (!tryDescend(way))
      throw new NoSuchElementException(way.toString());
    return this;
  }

  public TreeNavigator<COLOR, PAYLOAD, T> descendOr(COLOR way, Runnable ifNotPossible) {
    if (!tryDescend(way))
      ifNotPossible.run();
    return this;
  }

  public boolean tryDescend(COLOR way) {
    T tree = tree().sub(way);
    if (tree == null)
      return false;
    stack.push(tree);
    return true;
  }

  public boolean ascend() {
    stack.pop();
    return !stack.isEmpty();
  }

  public T tree() {
    return stack.peek();
  }

  public PAYLOAD get() {
    return tree().get();
  }
}
