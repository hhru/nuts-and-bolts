package ru.hh.nab.test.contract;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import ru.hh.nab.test.contract.model.Expectations;
import ru.hh.nab.test.contract.service.ExpectationsStorage;

public class ContractTestListener implements TestExecutionListener {

  @Override
  public void testPlanExecutionFinished(TestPlan testPlan) {
    Expectations expectations = ExpectationsStorage.getExpectations();
    if (expectations.getExpectations().isEmpty()) {
      return;
    }

    try {
      Path directoryPath = Paths.get(".", "target", "contracts");
      File directory = directoryPath.toFile();

      if (!directory.exists()) {
        directory.mkdirs();
      }
      File expectationsFile = directoryPath.resolve(Paths.get("expectations.json")).toFile();
      getWriter().writeValue(expectationsFile, expectations);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private ObjectWriter getWriter() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    return objectMapper.writer(new DefaultPrettyPrinter());
  }
}
