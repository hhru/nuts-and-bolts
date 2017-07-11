package ru.hh.nab.hibernate;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostCommitHooks {

  private final static Logger log = LoggerFactory.getLogger(PostCommitHooks.class);

  private final List<Runnable> hooks = new ArrayList<>();

  public static volatile boolean debug;

  public void addHook(Runnable task) {
    hooks.add(task);
  }

  public void execute() {
    for (Runnable action : hooks) {
      try {
        action.run();
      } catch (Exception e) {
        if (debug) {
          throw new RuntimeException(e.getMessage(), e);
        }
        log.error(e.getMessage(), e);
      }
    }
  }
}
