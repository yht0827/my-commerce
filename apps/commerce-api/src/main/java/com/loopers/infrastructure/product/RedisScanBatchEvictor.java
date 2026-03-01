package com.loopers.infrastructure.product;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

final class RedisScanBatchEvictor {

	private static final long DEFAULT_SCAN_COUNT = 500L;
	private static final int DEFAULT_DELETE_BATCH_SIZE = 200;

	private RedisScanBatchEvictor() {
	}

	static void evictByPattern(
		final RedisTemplate<String, Object> redisTemplate,
		final String pattern,
		final String failureMessage
	) {
		redisTemplate.execute((RedisCallback<Void>)connection -> {
			List<byte[]> deleteBatch = new ArrayList<>(DEFAULT_DELETE_BATCH_SIZE);
			ScanOptions scanOptions = ScanOptions.scanOptions()
				.match(pattern)
				.count(DEFAULT_SCAN_COUNT)
				.build();

			try (Cursor<byte[]> cursor = connection.scan(scanOptions)) {
				while (cursor.hasNext()) {
					deleteBatch.add(cursor.next());
					deleteBatchIfFull(connection, deleteBatch);
				}
				deleteBatch(connection, deleteBatch);
			} catch (Exception e) {
				throw new IllegalStateException(failureMessage, e);
			}

			return null;
		});
	}

	private static void deleteBatchIfFull(final RedisConnection connection, final List<byte[]> deleteBatch) {
		if (deleteBatch.size() < DEFAULT_DELETE_BATCH_SIZE) {
			return;
		}
		deleteBatch(connection, deleteBatch);
	}

	private static void deleteBatch(final RedisConnection connection, final List<byte[]> deleteBatch) {
		if (deleteBatch.isEmpty()) {
			return;
		}
		connection.del(deleteBatch.toArray(new byte[0][]));
		deleteBatch.clear();
	}
}
