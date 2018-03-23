package ru.hh.nab.datasource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {DataSourceTestConfig.class})
public class JdbcTemplateTest extends AbstractJUnit4SpringContextTests {
  @Inject
  private DataSource dataSource;
  @Inject
  private JdbcTemplate jdbcTemplate;

  @Before
  public void initDatasource() throws SQLException {
    Connection connection = dataSource.getConnection();
    connection.setAutoCommit(false);

    try (Statement stmt = connection.createStatement()) {
      stmt.execute("CREATE TABLE test_table (id INTEGER NOT NULL, data TEXT);");
      connection.commit();
    }
  }

  @Test
  public void testJdbcTemplate() {
    jdbcTemplate.execute("INSERT INTO test_table VALUES (1, 'test');");

    String result = jdbcTemplate.queryForObject(
      "SELECT * FROM test_table;",
      (resultSet, rowNum) -> resultSet.getInt("id") + ":" + resultSet.getString("data")
    );

    assertEquals("1:test", result);
  }
}
