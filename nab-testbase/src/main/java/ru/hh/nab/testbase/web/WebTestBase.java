package ru.hh.nab.testbase.web;

import jakarta.inject.Inject;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.hh.nab.testbase.extensions.SpringExtensionWithFailFast;

@ExtendWith({SpringExtensionWithFailFast.class})
public abstract class WebTestBase {

  @Inject
  protected ResourceHelper resourceHelper;
}
