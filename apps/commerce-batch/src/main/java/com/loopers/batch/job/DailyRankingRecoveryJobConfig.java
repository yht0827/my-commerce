package com.loopers.batch.job;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.loopers.support.ranking.RankingEventType;
import com.loopers.support.ranking.RankingKeyManger;
import com.loopers.support.ranking.RankingScoreCalculator;
import com.loopers.support.ranking.RankingType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DailyRankingRecoveryJobConfig {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final String JOB_NAME = "dailyRankingRecoveryJob";
	private static final String STEP_NAME = "dailyRankingRecoveryStep";
	private static final int CHUNK_SIZE = 100;
	private static final int DEFAULT_TOP_N = 100;
	private static final String RANK_TYPE = "DAILY";

	private static final String SELECT_RECOVERY_SQL = """
		SELECT pa.product_id AS product_id,
		       (pa.like_count * :likeWeight)
		       + (pa.view_count * :viewWeight)
		       + (COALESCE(ord.order_count, 0) * :orderWeight) AS weighted_score
		FROM product_aggregate pa
		LEFT JOIN (
			SELECT oi.product_id, COUNT(*) AS order_count
			FROM order_items oi
			JOIN orders o ON o.order_number = oi.order_id
			WHERE o.status = 'CONFIRMED'
			  AND o.created_at >= :startAt
			  AND o.created_at < :endAt
			GROUP BY oi.product_id
		) ord ON ord.product_id = pa.product_id
		ORDER BY weighted_score DESC, pa.product_id DESC
		LIMIT :topN
		""";

	private static final String DELETE_SNAPSHOT_SQL =
		"DELETE FROM product_ranking WHERE rank_type = :rankType AND rank_date = :rankDate";

	private static final String INSERT_SNAPSHOT_SQL = """
		INSERT INTO product_ranking (product_id, rank_type, rank_position, score, rank_date, created_at, updated_at)
		VALUES (:productId, :rankType, :rankPosition, :score, :rankDate, NOW(), NOW())
		""";

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final NamedParameterJdbcTemplate jdbc;
	private final StringRedisTemplate redisTemplate;
	private final RankingKeyManger rankingKeyManger;

	@Bean
	public Job dailyRankingRecoveryJob(final Step dailyRankingRecoveryStep) {
		return new JobBuilder(JOB_NAME, jobRepository)
			.incrementer(new RunIdIncrementer())
			.start(dailyRankingRecoveryStep)
			.build();
	}

	@Bean
	public Step dailyRankingRecoveryStep(
		final ItemReader<RecoveryRankingRow> dailyRankingRecoveryReader,
		final ItemWriter<RecoveryRankingRow> dailyRankingRecoveryWriter
	) {
		return new StepBuilder(STEP_NAME, jobRepository)
			.<RecoveryRankingRow, RecoveryRankingRow>chunk(CHUNK_SIZE, transactionManager)
			.reader(dailyRankingRecoveryReader)
			.writer(dailyRankingRecoveryWriter)
			.faultTolerant()
			.build();
	}

	@Bean
	@StepScope
	public ItemReader<RecoveryRankingRow> dailyRankingRecoveryReader(
		@Value("#{jobParameters['date']}") final String dateParam,
		@Value("#{jobParameters['topN']}") final Integer topN,
		final RankingScoreCalculator rankingScoreCalculator
	) {
		LocalDate rankDate = LocalDate.parse(dateParam);
		int limit = topN != null ? topN : DEFAULT_TOP_N;

		LocalDateTime startAtUtc = toUtcStartOfDay(rankDate);
		LocalDateTime endAtUtc = toUtcStartOfDay(rankDate.plusDays(1));

		MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("likeWeight", rankingScoreCalculator.calculateScore(RankingEventType.LIKE))
			.addValue("viewWeight", rankingScoreCalculator.calculateScore(RankingEventType.VIEW))
			.addValue("orderWeight", rankingScoreCalculator.calculateScore(RankingEventType.ORDER))
			.addValue("startAt", Timestamp.valueOf(startAtUtc))
			.addValue("endAt", Timestamp.valueOf(endAtUtc))
			.addValue("topN", limit);

		List<RecoveryRankingRow> rows = jdbc.query(
			SELECT_RECOVERY_SQL,
			params,
			(rs, rowNum) -> new RecoveryRankingRow(rs.getLong("product_id"), rs.getDouble("weighted_score"))
		);

		log.info("일간 랭킹 복구 reader 준비 완료 - date={}, rowCount={}, topN={}", rankDate, rows.size(), limit);
		return new ListItemReader<>(rows);
	}

	@Bean
	@StepScope
	public ItemWriter<RecoveryRankingRow> dailyRankingRecoveryWriter(
		@Value("#{jobParameters['date']}") final String dateParam,
		@Value("#{jobParameters['dryRun'] ?: false}") final boolean dryRun
	) {
		return new ItemWriter<>() {
			private boolean initialized = false;
			private long nextRank = 1L;

			@Override
			public void write(final Chunk<? extends RecoveryRankingRow> chunk) {
				if (chunk == null || chunk.isEmpty()) {
					return;
				}

				LocalDate rankDate = LocalDate.parse(dateParam);
				Date sqlDate = Date.valueOf(rankDate);
				String redisDailyKey = rankingKeyManger.getDailyRankingKey(rankDate);

				if (dryRun) {
					log.info("[DRY-RUN] 일간 랭킹 복구 예정 - date={}, count={}, rankRange={}..{}",
						rankDate, chunk.size(), nextRank, nextRank + chunk.size() - 1);
					nextRank += chunk.size();
					return;
				}

				if (!initialized) {
					redisTemplate.delete(redisDailyKey);
					redisTemplate.expire(redisDailyKey, Duration.ofSeconds(rankingKeyManger.getTTL(RankingType.DAILY)));

					int deletedCount = jdbc.update(DELETE_SNAPSHOT_SQL,
						new MapSqlParameterSource()
							.addValue("rankType", RANK_TYPE)
							.addValue("rankDate", sqlDate));

					log.info("일간 랭킹 복구 초기화 완료 - date={}, redisKey={}, deletedSnapshotCount={}",
						rankDate, redisDailyKey, deletedCount);
					initialized = true;
				}

				List<MapSqlParameterSource> snapshotRows = new ArrayList<>(chunk.size());
				for (RecoveryRankingRow row : chunk.getItems()) {
					double score = row.score() == null ? 0.0 : row.score();
					redisTemplate.opsForZSet().add(redisDailyKey, String.valueOf(row.productId()), score);

					snapshotRows.add(new MapSqlParameterSource()
						.addValue("productId", row.productId())
						.addValue("rankType", RANK_TYPE)
						.addValue("rankPosition", nextRank++)
						.addValue("score", score)
						.addValue("rankDate", sqlDate));
				}

				jdbc.batchUpdate(INSERT_SNAPSHOT_SQL, snapshotRows.toArray(MapSqlParameterSource[]::new));
				log.info("일간 랭킹 복구 chunk 저장 완료 - date={}, insertedCount={}", rankDate, chunk.size());
			}
		};
	}

	private LocalDateTime toUtcStartOfDay(final LocalDate localDate) {
		return localDate.atStartOfDay(KST)
			.withZoneSameInstant(ZoneOffset.UTC)
			.toLocalDateTime();
	}

	private record RecoveryRankingRow(Long productId, Double score) {
	}
}
