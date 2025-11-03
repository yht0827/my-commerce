package com.loopers.batch.job;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

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
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.loopers.batch.dto.ProductScoreRow;
import com.loopers.batch.dto.RankedProduct;
import com.loopers.support.ranking.RankingEventType;
import com.loopers.support.ranking.RankingScoreCalculator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DailyRankingJobConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final DataSource dataSource;
	private final NamedParameterJdbcTemplate jdbc;

	private static final String JOB_NAME = "dailyRankingJob";
	private static final String STEP_NAME = "dailyRankingStep";
	private static final String READER_NAME = "dailyReader";
	private static final int CHUNK_SIZE = 100;
	private static final int DEFAULT_TOP_N = 100;
	private static final String SELECT_DAILY_SQL = """
		SELECT product_id,
		       like_count,
		       view_count,
		       order_count,
		       (like_count * ?) + (view_count * ?) + (order_count * ?) AS weighted_score
		FROM product_metrics_daily
		WHERE snapshot_date = ?
		ORDER BY weighted_score DESC, product_id DESC
		LIMIT ?
		""";
	private static final String DELETE_SQL =
		"DELETE FROM product_rank_daily WHERE snapshot_date = :date";
	private static final String INSERT_SQL = """
		INSERT INTO product_rank_daily (snapshot_date, product_id, ranking, score, like_count, view_count, order_count, created_at, updated_at)
		VALUES (:date, :productId, :ranking, :score, :likeCount, :viewCount, :orderCount, NOW(), NOW())
		""";

	@Bean
	public Job dailyRankingJob(Step dailyRankingStep) {
		log.info("DailyRankingJob 배치 등록 완료");
		return new JobBuilder(JOB_NAME, jobRepository)
			.incrementer(new RunIdIncrementer())
			.start(dailyRankingStep)
			.build();
	}

	@Bean
	public Step dailyRankingStep(
		ItemReader<ProductScoreRow> dailyReader,
		ItemWriter<RankedProduct> dailyWriter
	) {
		log.info("일간 랭킹 step 구성 완료 - stepName={}, chunkSize={}", STEP_NAME, CHUNK_SIZE);
		return new StepBuilder(STEP_NAME, jobRepository)
			.<ProductScoreRow, RankedProduct>chunk(CHUNK_SIZE, transactionManager)
			.reader(dailyReader)
			.processor(this::toRankedProduct)
			.writer(dailyWriter)
			.faultTolerant()
			.build();
	}

	@Bean
	@StepScope
	public ItemReader<ProductScoreRow> dailyReader(
		@Value("#{jobParameters['date']}") String dateStr,
		@Value("#{jobParameters['topN']}") Integer topN,
		RankingScoreCalculator rankingScoreCalculator
	) {
		LocalDate date = LocalDate.parse(dateStr);
		int limit = topN != null ? topN : DEFAULT_TOP_N;

		double likeWeight = rankingScoreCalculator.calculateScore(RankingEventType.LIKE);
		double viewWeight = rankingScoreCalculator.calculateScore(RankingEventType.VIEW);
		double orderWeight = rankingScoreCalculator.calculateScore(RankingEventType.ORDER);

		log.info("일간 랭킹 reader 구성 완료 - date={}, topN={} (기본 {}), weights=[like:{}, view:{}, order:{}]",
			date, limit, DEFAULT_TOP_N, likeWeight, viewWeight, orderWeight);

		return new JdbcCursorItemReaderBuilder<ProductScoreRow>()
			.name(READER_NAME)
			.dataSource(dataSource)
			.sql(SELECT_DAILY_SQL)
			.preparedStatementSetter(ps -> {
				ps.setDouble(1, likeWeight);
				ps.setDouble(2, viewWeight);
				ps.setDouble(3, orderWeight);
				ps.setDate(4, Date.valueOf(date));
				ps.setInt(5, limit);
			})
			.rowMapper((rs, rowNum) -> new ProductScoreRow(
				rs.getLong("product_id"),
				rs.getLong("like_count"),
				rs.getLong("view_count"),
				rs.getLong("order_count"),
				rs.getDouble("weighted_score")))
			.build();
	}

	/**
	 * 멱등 Writer: 대상 date 선삭제 후 순위 번호 증가하며 일괄 INSERT.
	 * dryRun=true면 쓰기 생략하고 로그만 출력
	 */
	@Bean
	@StepScope
	public ItemWriter<RankedProduct> dailyWriter(
		@Value("#{jobParameters['date']}") String dateParam,
		@Value("#{jobParameters['dryRun'] ?: false}") boolean dryRun
	) {
		return new ItemWriter<>() {
			private boolean deleted = false;
			private int nextRank = 1;

			@Override
			public void write(Chunk<? extends RankedProduct> chunk) {
				if (chunk == null || chunk.isEmpty())
					return;

				LocalDate date = LocalDate.parse(dateParam);
				Date sqlDate = Date.valueOf(date);

				if (dryRun) {
					log.info("[DRY-RUN] 일간 스냅샷 저장 예정 - date={}, 건수={}, 순위범위={}..{}",
						date, chunk.size(), nextRank, nextRank + chunk.size() - 1);
					nextRank += chunk.size();
					return;
				}

				if (!deleted) {
					int del = jdbc.update(DELETE_SQL,
						new MapSqlParameterSource().addValue("date", sqlDate));
					log.info("product_rank_daily 선 삭제 완료 - date={}, 삭제행수={}", date, del);
					deleted = true;
				}

				List<MapSqlParameterSource> batch = new ArrayList<>(chunk.size());
				for (RankedProduct row : chunk.getItems()) {
					batch.add(new MapSqlParameterSource()
						.addValue("date", sqlDate)
						.addValue("productId", row.productId())
						.addValue("ranking", nextRank++)
						.addValue("score", row.score())
						.addValue("likeCount", row.likeCount())
						.addValue("viewCount", row.viewCount())
						.addValue("orderCount", row.orderCount()));
				}
				jdbc.batchUpdate(INSERT_SQL, batch.toArray(MapSqlParameterSource[]::new));
				log.info("product_rank_daily 적재 완료 - date={}, 저장행수={}", date, chunk.size());
			}
		};
	}

	private RankedProduct toRankedProduct(ProductScoreRow row) {
		return new RankedProduct(
			row.productId(),
			row.weightedScore(),
			row.likeCount(),
			row.viewCount(),
			row.orderCount()
		);
	}
}
