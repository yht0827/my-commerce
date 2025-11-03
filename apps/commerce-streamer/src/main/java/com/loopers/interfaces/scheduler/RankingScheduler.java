package com.loopers.interfaces.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.loopers.domain.metrics.RankingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

	private final RankingService rankingService;
	private final JobLauncher jobLauncher;
	private final Job dailyRankingJob;
	private final Job weeklyRankingJob;
	private final Job monthlyRankingJob;

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int DEFAULT_TOP_N = 100;
	private static final DateTimeFormatter WEEKLY_PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter MONTHLY_PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

	/**
	 * 매일 23시 50분에 전날의 랭킹 데이터를 다음날 랭킹으로 이월
	 */
	@Scheduled(cron = "0 50 23 * * ?") // 23시 50분 실행
	public void carryOverScores() {
		log.info("랭킹 점수 스케줄러 실행");
		rankingService.carryOverRankingScores();
		log.info("랭킹 점수 스케줄러 완료");
	}

	// 매일 00:10, KST (전일 기준 실행)
	@Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
	public void runDaily() {
		LocalDate target = LocalDate.now(KST).minusDays(1);
		run(target, DEFAULT_TOP_N, false);
	}

	@Scheduled(cron = "0 20 0 * * MON", zone = "Asia/Seoul")
	public void runWeeklyScheduled() {
		LocalDate today = LocalDate.now(KST);
		LocalDate end = today.minusDays(1);
		LocalDate start = end.minusDays(6);
		runWeekly(start, end, DEFAULT_TOP_N, false);
	}

	@Scheduled(cron = "0 40 0 1 * *", zone = "Asia/Seoul")
	public void runMonthlyScheduled() {
		LocalDate today = LocalDate.now(KST);
		LocalDate end = today.minusDays(1);
		LocalDate start = end.withDayOfMonth(1);
		runMonthly(start, end, DEFAULT_TOP_N, false);
	}

	// 필요할 때 수동 호출
	public void run(LocalDate date, int topN, boolean dryRun) {
		JobParameters params = new JobParametersBuilder()
			.addString("date", date.toString())
			.addLong("topN", (long)topN)
			.addString("dryRun", Boolean.toString(dryRun))
			.toJobParameters();
		runJob(dailyRankingJob, params, "일간", "date=" + date + ", topN=" + topN + ", dryRun=" + dryRun);
	}

	public void runWeekly(LocalDate startDate, LocalDate endDate, int topN, boolean dryRun) {

		String periodKey = startDate.format(WEEKLY_PERIOD_FORMAT);
		JobParameters params = new JobParametersBuilder()
			.addString("startDate", startDate.toString())
			.addString("endDate", endDate.toString())
			.addString("periodKey", periodKey)
			.addLong("topN", (long)topN)
			.addString("dryRun", Boolean.toString(dryRun))
			.toJobParameters();
		runJob(weeklyRankingJob, params, "주간",
			"startDate=" + startDate + ", endDate=" + endDate + ", topN=" + topN + ", dryRun=" + dryRun);
	}

	public void runMonthly(LocalDate startDate, LocalDate endDate, int topN, boolean dryRun) {

		String periodKey = startDate.format(MONTHLY_PERIOD_FORMAT);
		JobParameters params = new JobParametersBuilder()
			.addString("startDate", startDate.toString())
			.addString("endDate", endDate.toString())
			.addString("periodKey", periodKey)
			.addLong("topN", (long)topN)
			.addString("dryRun", Boolean.toString(dryRun))
			.toJobParameters();
		runJob(monthlyRankingJob, params, "월간",
			"startDate=" + startDate + ", endDate=" + endDate + ", topN=" + topN + ", dryRun=" + dryRun);
	}

	private void runJob(Job job, JobParameters params, String label, String detail) {
		try {
			jobLauncher.run(job, params);
		} catch (Exception e) {
			log.error("{} 랭킹 배치 실패 ({})", label, detail, e);
		}
	}

}
