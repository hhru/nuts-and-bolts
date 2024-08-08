package ru.hh.nab.hibernate.monitoring;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import static ru.hh.nab.hibernate.monitoring.HibernateMetrics.QUERY_PLAN_CACHE_HIT_COUNT;
import static ru.hh.nab.hibernate.monitoring.HibernateMetrics.QUERY_PLAN_CACHE_MISS_COUNT;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import ru.hh.nab.metrics.TaggedSender;

public class HibernateStatisticsSender {

  private static final String HIBERNATE_GENERATE_STATISTICS_PROPERTY = "hibernate.generate_statistics";
  private static final String SESSION_FACTORY_NAME_TAG = "factory";

  public HibernateStatisticsSender(
      Properties hibernateProperties,
      String serviceName,
      Map<String, SessionFactory> sessionFactories,
      StatsDSender statsDSender
  ) {
    if (!Optional.ofNullable(hibernateProperties.getProperty(HIBERNATE_GENERATE_STATISTICS_PROPERTY)).orElse("").equals("true")) {
      return;
    }

    sessionFactories.forEach((sessionFactoryName, sessionFactory) -> {
      var sender = new TaggedSender(
          statsDSender,
          Set.of(new Tag(Tag.APP_TAG_NAME, serviceName), new Tag(SESSION_FACTORY_NAME_TAG, sessionFactoryName))
      );

      statsDSender.sendPeriodically(() -> {
        Statistics statistics = sessionFactory.getStatistics();

        sender.sendCount(QUERY_PLAN_CACHE_HIT_COUNT, statistics.getQueryPlanCacheHitCount());
        sender.sendCount(QUERY_PLAN_CACHE_MISS_COUNT, statistics.getQueryPlanCacheMissCount());

        statistics.clear();
      });
    });
  }
}
