package com.loopers.interfaces.scheduler;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.loopers.support.ranking.RankingKeyManger;
import com.loopers.support.ranking.RankingType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int DEFAULT_TOP_N = 100;

	private final StringRedisTemplate redisTemplate;
	private final RankingKeyManger rankingKeyManger;
	private final JobLauncher jobLauncher;
	private final Job dailyRankingJob;
	private final Job dailyRankingRecoveryJob;
	private final Job weeklyRankingJob;
	private final Job monthlyRankingJob;

	@Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
	public void runDailyScheduled() {
		LocalDate rankDate = LocalDate.now(KST).minusDays(1);
		runDaily(rankDate, DEFAULT_TOP_N, false);
	}

	@Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
	public void runWeeklyScheduled() {
		LocalDate rankDate = LocalDate.now(KST).minusDays(1);
		LocalDate startDate = rankDate.minusDays(6);

		refreshRollingRanking(startDate, rankDate, rankingKeyManger.getWeeklyRankingKey(), RankingType.WEEKLY, "주간");
		runWeekly(rankDate, DEFAULT_TOP_N, false);
	}

	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
	public void runMonthlyScheduled() {
		LocalDate rankDate = LocalDate.now(KST).minusDays(1);
		LocalDate startDate = rankDate.minusDays(29);

		refreshRollingRanking(startDate, rankDate, rankingKeyManger.getMonthlyRankingKey(), RankingType.MONTHLY, "월간");
		runMonthly(rankDate, DEFAULT_TOP_N, false);
	}

	@Scheduled(cron = "${ranking.recovery.cron:-}", zone = "Asia/Seoul")
	public void runDailyRecoveryScheduled() {
		LocalDate rankDate = LocalDate.now(KST).minusDays(1);
		runDailyRecovery(rankDate, DEFAULT_TOP_N, false);
	}

	public void runDaily(final LocalDate rankDate, final int topN, final boolean dryRun) {
		JobParameters params = new JobParametersBuilder()
			.addString("date", rankDate.toString())
			.addLong("topN", (long)topN)
			.addString("dryRun", Boolean.toString(dryRun))
			.toJobParameters();

		runJob(dailyRankingJob, params, "일간", "rankDate=" + rankDate + ", topN=" + topN + ", dryRun=" + dryRun);
	}

	public void runWeekly(final LocalDate rankDate, final int topN, final boolean dryRun) {
		JobParameters params = new JobParametersBuilder()
			.addString("rankDate", rankDate.toString())
			.addLong("topN", (long)topN)
			.addString("dryRun", Boolean.toString(dryRun))
			.toJobParameters();

		runJob(weeklyRankingJob, params, "주간", "rankDate=" + rankDate + ", topN=" + topN + ", dryRun=" + dryRun);
	}

	public void runDailyRecovery(final LocalDate rankDate, final int topN, final boolean dryRun) {
		JobParameters params = new JobParametersBuilder()
			.addString("date", rankDate.toString())
			.addLong("topN", (long)topN)
			.addString("dryRun", Boolean.toString(dryRun))
			.toJobParameters();

		runJob(dailyRankingRecoveryJob, params,
			"일간복구",
			"rankDate=" + rankDate + ", topN=" + topN + ", dryRun=" + dryRun);
	}

	public void runMonthly(final LocalDate rankDate, final int topN, final boolean dryRun) {
		JobParameters params = new JobParametersBuilder()
			.addString("rankDate", rankDate.toString())
			.addLong("topN", (long)topN)
			.addString("dryRun", Boolean.toString(dryRun))
			.toJobParameters();

		runJob(monthlyRankingJob, params, "월간", "rankDate=" + rankDate + ", topN=" + topN + ", dryRun=" + dryRun);
	}

	private void refreshRollingRanking(
		final LocalDate startDate,
		final LocalDate endDate,
		final String targetKey,
		final RankingType rankingType,
		final String label
	) {
		if (endDate.isBefore(startDate)) {
			log.warn("{} 랭킹 Redis 집계 구간이 유효하지 않아 건너뜁니다. startDate={}, endDate={}", label, startDate, endDate);
			return;
		}

		List<String> dailyKeys = new ArrayList<>();
		LocalDate cursor = startDate;
		while (!cursor.isAfter(endDate)) {
			dailyKeys.add(rankingKeyManger.getDailyRankingKey(cursor));
			cursor = cursor.plusDays(1);
		}

		if (dailyKeys.isEmpty()) {
			return;
		}

		String firstSourceKey = dailyKeys.get(0);
		Set<String> sourceKeys = new LinkedHashSet<>(dailyKeys.subList(1, dailyKeys.size()));
		redisTemplate.opsForZSet().unionAndStore(firstSourceKey, sourceKeys, targetKey);
		redisTemplate.expire(targetKey, Duration.ofSeconds(rankingKeyManger.getTTL(rankingType)));

		log.info("{} 랭킹 Redis 집계 완료 - startDate={}, endDate={}, targetKey={}, sourceCount={}",
			label, startDate, endDate, targetKey, dailyKeys.size());
	}

	private void runJob(final Job job, final JobParameters params, final String label, final String detail) {
		try {
			jobLauncher.run(job, params);
		} catch (Exception e) {
			log.error("{} 랭킹 배치 실패 ({})", label, detail, e);
		}
	}
}
