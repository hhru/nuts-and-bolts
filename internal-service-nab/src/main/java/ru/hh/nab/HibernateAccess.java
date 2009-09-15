package ru.hh.nab;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public class HibernateAccess {
  private final SessionFactory hiber;
  private final DataSource ds;

  HibernateAccess(SessionFactory hiber, DataSource ds) {
    this.hiber = hiber;
    this.ds = ds;
  }

  public <T, E extends Throwable> T perform(HibernateCheckedAction<T, E> action) throws E, SQLException {
    Connection conn = ds.getConnection();
    try {
      Session s = hiber.openSession(conn);
      try {
        Transaction tx = s.beginTransaction();
        try {
          return action.perform(s);
        } finally {
          tx.commit();
        }
      } finally {
        s.close();
      }
    } finally {
      conn.close();
    }
  }

  public <T> T perform(HibernateAction<T> action) throws SQLException {
    Connection conn = ds.getConnection();
    try {
      Session s = hiber.openSession(conn);
      try {
        Transaction tx = s.beginTransaction();
        try {
          return action.perform(s);
        } finally {
          tx.commit();
        }
      } finally {
        s.close();
      }
    } finally {
      conn.close();
    }
  }
}
