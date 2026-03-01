package com.loopers.infrastructure.ranking;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.loopers.domain.ranking.RankingItem;
import com.loopers.domain.ranking.RankingPeriod;
import com.loopers.domain.ranking.RankingSnapshot;

@Repository
public interface RankingSnapshotJpaRepository extends JpaRepository<RankingSnapshot, Long> {

	@Query("""
			select new com.loopers.domain.ranking.RankingItem(
				r.rankPosition,
				r.productId,
				r.score,
				null,
				null,
				null
			)
			from RankingSnapshot r
			where r.rankType = :rankType
				and r.rankDate = :rankDate
			order by r.rankPosition asc
		""")
	Page<RankingItem> findByRankTypeAndRankDate(
		@Param("rankType") RankingPeriod rankType,
		@Param("rankDate") LocalDate rankDate,
		Pageable pageable
	);
}
