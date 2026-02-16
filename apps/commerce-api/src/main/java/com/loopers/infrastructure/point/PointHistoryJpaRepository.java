package com.loopers.infrastructure.point;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.point.PointHistory;
import com.loopers.domain.user.UserId;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistory, Long> {

	List<PointHistory> findAllByUserIdOrderByCreatedAtDesc(UserId userId);
}
