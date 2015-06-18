package ru.hh.nab.hibernate;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

// TypedQuery.getSingleResult() is poorly implemented in Hibernate3
// and not exactly what we want from it in Hibernate4

public class SingleQueryResultUtil {

  private SingleQueryResultUtil() {}

  public static <T> T getSingleResult(TypedQuery<T> query, String resultDesc) {
    return getUnique(query, resultDesc, null, false);
  }

  public static <T> T getSingleResultOrNull(TypedQuery<T> query, String resultDesc) {
    return getUnique(query, resultDesc, null, true);
  }

  public static <T> T getSingleResult(TypedQuery<T> query, Class<T> clazz) {
    return getUnique(query, null, clazz, false);
  }

  public static <T> T getSingleResultOrNull(TypedQuery<T> query, Class<T> clazz) {
    return getUnique(query, null, clazz, true);
  }

  // throw exception if not unique, return unique result if available,
  // otherwise return null of throw exception
  private static <T> T getUnique(TypedQuery<T> query, String resultDesc, Class<T> clazz, boolean returnNullIfEmpty) {
    T result = null;
    boolean resultAssigned = false;
    for (T fetchedResult : query.getResultList()) {
      if (!resultAssigned) {
        result = fetchedResult;
        resultAssigned = true;
      } else if (fetchedResult != result) {
        // each session must have at most one object representing the same entity
        throw new NonUniqueResultException("query returns more than one unique " + (resultDesc != null ? resultDesc : clazz.getSimpleName()));
      }
    }
    if (resultAssigned || returnNullIfEmpty) {
      return result;
    } else {
      throw new NoResultException("Query returned no results for " + (resultDesc != null ? resultDesc : clazz.getSimpleName()));
    }
  }
}
