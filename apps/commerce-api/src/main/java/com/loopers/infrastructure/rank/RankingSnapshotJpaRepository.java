package com.loopers.infrastructure.rank;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.loopers.domain.rank.RankingDailySnapshot;
import com.loopers.domain.rank.RankingItem;

@Repository
public interface RankingSnapshotJpaRepository extends JpaRepository<RankingDailySnapshot, Long> {

	@Query("""
			select new com.loopers.domain.rank.RankingItem(
				r.ranking,
				r.productId,
				r.score,
				r.likeCount,
				r.viewCount,
				r.orderCount
			)
			from RankingDailySnapshot r
			where r.snapshotDate = :date
			order by r.ranking asc
		""")
	Page<RankingItem> findDaily(@Param("date") LocalDate date, Pageable pageable);

	@Query("""
			select new com.loopers.domain.rank.RankingItem(
				w.ranking,
				w.productId,
				w.score,
				w.likeCount,
				w.viewCount,
				w.orderCount
			)
			from RankingWeeklySnapshot w
			where w.periodKey = :periodKey
			order by w.ranking asc
		""")
	Page<RankingItem> findWeekly(@Param("periodKey") String periodKey, Pageable pageable);

	@Query("""
			select new com.loopers.domain.rank.RankingItem(
				m.rank,
				m.productId,
				m.score,
				m.likeCount,
				m.viewCount,
				m.orderCount
			)
			from RankingMonthlySnapshot m
			where m.periodKey = :periodKey
			order by m.rank asc
		""")
	Page<RankingItem> findMonthly(@Param("periodKey") String periodKey, Pageable pageable);
}
