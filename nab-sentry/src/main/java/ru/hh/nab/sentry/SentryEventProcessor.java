package ru.hh.nab.sentry;

import io.sentry.EventProcessor;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import java.util.List;
import java.util.Optional;
import ru.hh.nab.common.mdc.MDC;
import static ru.hh.nab.common.mdc.MDC.CONTROLLER_MDC_KEY;

public class SentryEventProcessor implements EventProcessor {

  @Override
  public SentryEvent process(SentryEvent event, Hint hint) {
    MDC.getController().ifPresent(controller -> {
      if (Optional.ofNullable(event.getFingerprints()).map(List::isEmpty).orElse(false)) {
        event.setFingerprints(List.of("{{ default }}", controller));
      } else {
        List<String> fingerprints = event.getFingerprints();
        if (!fingerprints.contains(controller)) {
          fingerprints.add(controller);
          event.setFingerprints(fingerprints);
        }
      }
      event.setTag(CONTROLLER_MDC_KEY, controller);
    });
    return event;
  }
}
