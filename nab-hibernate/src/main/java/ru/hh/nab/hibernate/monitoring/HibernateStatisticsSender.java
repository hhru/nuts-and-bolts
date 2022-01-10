package ru.hh.nab.hibernate.monitoring;

import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import javax.inject.Named;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import static ru.hh.nab.hibernate.monitoring.HibernateMetrics.QUERY_PLAN_CACHE_HIT_COUNT;
import static ru.hh.nab.hibernate.monitoring.HibernateMetrics.QUERY_PLAN_CACHE_MISS_COUNT;
import ru.hh.nab.hibernate.qualifier.Hibernate;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import ru.hh.nab.metrics.TaggedSender;

public class HibernateStatisticsSender {

  private final static String HIBERNATE_GENERATE_STATISTICS_PROPERTY = "hibernate.generate_statistics";

  public HibernateStatisticsSender(@Hibernate Properties hibernateProperties, @Named(SERVICE_NAME) String serviceName, SessionFactory sessionFactory,
                                   StatsDSender statsDSender) {
    if (!Optional.ofNullable(hibernateProperties.getProperty(HIBERNATE_GENERATE_STATISTICS_PROPERTY)).orElse("").equals("true")) {
      return;
    }

    var sender = new TaggedSender(statsDSender, Set.of(new Tag(Tag.APP_TAG_NAME, serviceName)));

    statsDSender.sendPeriodically(() -> {
      Statistics statistics = sessionFactory.getStatistics();

      sender.sendCount(QUERY_PLAN_CACHE_HIT_COUNT, statistics.getQueryPlanCacheHitCount());
      sender.sendCount(QUERY_PLAN_CACHE_MISS_COUNT, statistics.getQueryPlanCacheMissCount());

      statistics.clear();
    });
  }
}
