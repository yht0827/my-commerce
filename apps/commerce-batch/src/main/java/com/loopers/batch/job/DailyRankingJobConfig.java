package com.loopers.batch.job;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.loopers.batch.dto.RankedProduct;
import com.loopers.support.ranking.RankingKeyManger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DailyRankingJobConfig {

	private static final String JOB_NAME = "dailyRankingJob";
	private static final String STEP_NAME = "dailyRankingStep";
	private static final int CHUNK_SIZE = 100;
	private static final int DEFAULT_TOP_N = 100;
	private static final String RANK_TYPE = "DAILY";
	private static final String DELETE_SQL =
		"DELETE FROM product_ranking WHERE rank_type = :rankType AND rank_date = :rankDate";
	private static final String INSERT_SQL = """
		INSERT INTO product_ranking (product_id, rank_type, rank_position, score, rank_date, created_at, updated_at)
		VALUES (:productId, :rankType, :rankPosition, :score, :rankDate, NOW(), NOW())
		""";

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final NamedParameterJdbcTemplate jdbc;
	private final StringRedisTemplate redisTemplate;
	private final RankingKeyManger rankingKeyManger;

	@Bean
	public Job dailyRankingJob(final Step dailyRankingStep) {
		return new JobBuilder(JOB_NAME, jobRepository)
			.incrementer(new RunIdIncrementer())
			.start(dailyRankingStep)
			.build();
	}

	@Bean
	public Step dailyRankingStep(
		final ItemReader<RankedProduct> dailyReader,
		final ItemWriter<RankedProduct> dailyWriter
	) {
		return new StepBuilder(STEP_NAME, jobRepository)
			.<RankedProduct, RankedProduct>chunk(CHUNK_SIZE, transactionManager)
			.reader(dailyReader)
			.writer(dailyWriter)
			.faultTolerant()
			.build();
	}

	@Bean
	@StepScope
	public ItemReader<RankedProduct> dailyReader(
		@Value("#{jobParameters['date']}") final String dateParam,
		@Value("#{jobParameters['topN']}") final Integer topN
	) {
		LocalDate date = LocalDate.parse(dateParam);
		int limit = topN != null ? topN : DEFAULT_TOP_N;
		String redisKey = rankingKeyManger.getDailyRankingKey(date);

		Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
			.reverseRangeWithScores(redisKey, 0, limit - 1L);
		List<RankedProduct> rows = toRankedProducts(tuples);

		log.info("일간 랭킹 reader 준비 완료 - date={}, redisKey={}, rowCount={}", date, redisKey, rows.size());
		return new ListItemReader<>(rows);
	}

	@Bean
	@StepScope
	public ItemWriter<RankedProduct> dailyWriter(
		@Value("#{jobParameters['date']}") final String dateParam,
		@Value("#{jobParameters['dryRun'] ?: false}") final boolean dryRun
	) {
		return new ItemWriter<>() {
			private boolean deleted = false;
			private long nextRank = 1L;

			@Override
			public void write(final Chunk<? extends RankedProduct> chunk) {
				if (chunk == null || chunk.isEmpty()) {
					return;
				}

				LocalDate rankDate = LocalDate.parse(dateParam);
				Date sqlDate = Date.valueOf(rankDate);

				if (dryRun) {
					log.info("[DRY-RUN] 일간 랭킹 저장 예정 - rankDate={}, count={}, rankRange={}..{}",
						rankDate, chunk.size(), nextRank, nextRank + chunk.size() - 1);
					nextRank += chunk.size();
					return;
				}

				if (!deleted) {
					int deletedCount = jdbc.update(DELETE_SQL,
						new MapSqlParameterSource()
							.addValue("rankType", RANK_TYPE)
							.addValue("rankDate", sqlDate));
					log.info("product_ranking 선삭제 완료 - rankType={}, rankDate={}, deletedCount={}",
						RANK_TYPE, rankDate, deletedCount);
					deleted = true;
				}

				List<MapSqlParameterSource> params = new ArrayList<>(chunk.size());
				for (RankedProduct row : chunk.getItems()) {
					params.add(new MapSqlParameterSource()
						.addValue("productId", row.productId())
						.addValue("rankType", RANK_TYPE)
						.addValue("rankPosition", nextRank++)
						.addValue("score", row.score())
						.addValue("rankDate", sqlDate));
				}

				jdbc.batchUpdate(INSERT_SQL, params.toArray(MapSqlParameterSource[]::new));
				log.info("product_ranking 적재 완료 - rankType={}, rankDate={}, insertedCount={}",
					RANK_TYPE, rankDate, chunk.size());
			}
		};
	}

	private List<RankedProduct> toRankedProducts(final Set<ZSetOperations.TypedTuple<String>> tuples) {
		if (tuples == null || tuples.isEmpty()) {
			return List.of();
		}

		List<RankedProduct> rows = new ArrayList<>(tuples.size());
		for (ZSetOperations.TypedTuple<String> tuple : tuples) {
			if (tuple == null || tuple.getValue() == null || tuple.getScore() == null) {
				continue;
			}

			try {
				rows.add(new RankedProduct(Long.parseLong(tuple.getValue()), tuple.getScore(), null, null, null));
			} catch (NumberFormatException e) {
				log.warn("유효하지 않은 productId 랭킹 데이터를 건너뜁니다. member={}", tuple.getValue());
			}
		}
		return rows;
	}
}
