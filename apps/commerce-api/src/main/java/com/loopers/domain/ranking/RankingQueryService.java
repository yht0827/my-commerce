package com.loopers.domain.ranking;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RankingQueryService {

	private final RankingValidator rankingValidator;
	private final RedisRankingRepository redisRankingRepository;
	private final RankingSnapshotRepository rankingSnapshotRepository;

	public RankingQueryService(
		final RankingValidator rankingValidator,
		final RedisRankingRepository redisRankingRepository,
		final RankingSnapshotRepository rankingSnapshotRepository
	) {
		this.rankingValidator = rankingValidator;
		this.redisRankingRepository = redisRankingRepository;
		this.rankingSnapshotRepository = rankingSnapshotRepository;
	}

	public RankingInfo.RankingPage getRanking(final RankingData.GetRanking data) {
		LocalDate validatedDate = rankingValidator.validateDate(data.date());
		RankingPeriod period = rankingValidator.validatePeriod(data.period());

		RankingPageData pageData = switch (period) {
			case DAILY -> getDaily(validatedDate, data.pageable());
			case WEEKLY -> getWeekly(validatedDate, data.pageable());
			case MONTHLY -> getMonthly(validatedDate, data.pageable());
		};

		Page<RankingItem> page = pageData.page() == null ? Page.empty(data.pageable()) : pageData.page();
		return RankingInfo.RankingPage.from(page, pageData.source());
	}

	private RankingPageData getDaily(final LocalDate validatedDate, final Pageable pageable) {
		try {
			Page<RankingItem> realtimePage = redisRankingRepository.fetchDaily(validatedDate, pageable);
			if (realtimePage != null && !realtimePage.isEmpty()) {
				return new RankingPageData(realtimePage, RankingSource.REALTIME);
			}
		} catch (Exception e) {
			log.warn("Daily ranking Redis 조회 실패 - date={}, page={}, size={}",
				validatedDate, pageable.getPageNumber(), pageable.getPageSize(), e);
		}

		log.debug("Daily ranking fallback to snapshot - date={}, page={}, size={}",
			validatedDate, pageable.getPageNumber(), pageable.getPageSize());
		Page<RankingItem> snapshotPage = rankingSnapshotRepository.fetchDaily(validatedDate, pageable);
		return new RankingPageData(snapshotPage, RankingSource.SNAPSHOT);
	}

	private RankingPageData getWeekly(final LocalDate validatedDate, final Pageable pageable) {
		try {
			Page<RankingItem> realtimePage = redisRankingRepository.fetchWeekly(pageable);
			if (realtimePage != null && !realtimePage.isEmpty()) {
				return new RankingPageData(realtimePage, RankingSource.REALTIME);
			}
		} catch (Exception e) {
			log.warn("Weekly ranking Redis 조회 실패 - date={}, page={}, size={}",
				validatedDate, pageable.getPageNumber(), pageable.getPageSize(), e);
		}

		Page<RankingItem> page = rankingSnapshotRepository.fetchWeekly(validatedDate, pageable);
		return new RankingPageData(page, RankingSource.SNAPSHOT);
	}

	private RankingPageData getMonthly(final LocalDate validatedDate, final Pageable pageable) {
		try {
			Page<RankingItem> realtimePage = redisRankingRepository.fetchMonthly(pageable);
			if (realtimePage != null && !realtimePage.isEmpty()) {
				return new RankingPageData(realtimePage, RankingSource.REALTIME);
			}
		} catch (Exception e) {
			log.warn("Monthly ranking Redis 조회 실패 - date={}, page={}, size={}",
				validatedDate, pageable.getPageNumber(), pageable.getPageSize(), e);
		}

		Page<RankingItem> page = rankingSnapshotRepository.fetchMonthly(validatedDate, pageable);
		return new RankingPageData(page, RankingSource.SNAPSHOT);
	}

	public Long getProductRanking(Long productId) {
		if (productId == null) {
			return null;
		}
		LocalDate today = rankingValidator.validateDate(LocalDate.now());
		return redisRankingRepository.fetchDailyRank(productId, today);
	}
}
