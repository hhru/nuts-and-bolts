package ru.hh.nab.metrics;

import java.util.Collection;

public class TaggedSender {
    private final Tag[] tags;
    private final StatsDSender delegate;


    public TaggedSender(StatsDSender delegate, Collection<Tag> tags) {
        this.tags = tags.toArray(Tag[]::new);
        this.delegate = delegate;
    }

    public void sendTime(String metricName, long value) {
        delegate.sendTime(metricName, value, tags);
    }

    public void sendCount(String metricName, long delta) {
        delegate.sendCount(metricName, delta, tags);
    }

    public void sendGauge(String metricName, long metric) {
        delegate.sendGauge(metricName, metric, tags);
    }

    public void sendMax(String metricName, Max max) {
        delegate.sendMax(metricName, max, tags);
    }

    public void sendHistogram(String metricName, Histogram histogram, int... percentiles) {
        delegate.sendHistogram(metricName, tags, histogram, percentiles);
    }

    public void sendMoments(String metricName, Moments moments) {
        delegate.sendMoments(metricName, moments, tags);
    }
}
