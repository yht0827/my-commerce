package com.loopers.domain.metrics;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ProductMetricsDailyRepository {

    List<ProductMetricsDaily> findAllBySnapshotDateAndProductIdIn(final LocalDate date, final Collection<Long> ids);

    void saveAll(final Iterable<ProductMetricsDaily> entities);
}

