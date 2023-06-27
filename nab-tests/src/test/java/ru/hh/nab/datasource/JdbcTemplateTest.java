package ru.hh.nab.datasource;

import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DataSourceTestConfig.class})
public class JdbcTemplateTest {
  @Inject
  private DataSource dataSource;
  @Inject
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
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
