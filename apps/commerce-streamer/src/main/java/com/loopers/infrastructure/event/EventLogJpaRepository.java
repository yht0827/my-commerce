package com.loopers.infrastructure.event;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.event.EventLog;

public interface EventLogJpaRepository extends JpaRepository<EventLog, Long> {

}
