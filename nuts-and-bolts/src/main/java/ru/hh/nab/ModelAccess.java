package ru.hh.nab;

import com.google.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

public class ModelAccess {
  private final Provider<EntityManagerFactory> emf;

  ModelAccess(Provider<EntityManagerFactory> emProvider) {
    this.emf = emProvider;
  }

  public <T, E extends Throwable> T perform(ModelCheckedAction<T, E> action) throws E {
    EntityManager em = emf.get().createEntityManager();
    try {
      EntityTransaction tx = em.getTransaction();
      tx.begin();
      try {
        return action.perform(em);
      } finally {
        tx.commit();
      }
    } finally {
      em.close();
    }
  }

  public <T> T perform(ModelAction<T> action) {
    EntityManager em = emf.get().createEntityManager();
    try {
      EntityTransaction tx = em.getTransaction();
      tx.begin();
      try {
        return action.perform(em);
      } finally {
        tx.commit();
      }
    } finally {
      em.close();
    }
  }
}
