package com.loopers.domain.event;

import java.util.List;

public interface EventLogRepository {

	void save(EventLog eventLog);
	
	void saveAll(List<EventLog> eventLogs);
}
