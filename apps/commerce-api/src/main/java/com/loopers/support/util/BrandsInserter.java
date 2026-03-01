package com.loopers.support.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class BrandsInserter {

	// 브랜드명 데이터
	private static final String[] BRAND_NAMES = {
		"Apple", "Samsung", "Google", "Microsoft", "Sony", "LG", "Huawei",
		"Xiaomi", "OnePlus", "Oppo", "Vivo", "Realme", "Nothing", "Asus",
		"나이키", "아디다스", "푸마", "리복", "언더아머", "뉴발란스"
	};

	public static void execute(int recordCount) throws Exception {
		System.out.println("=== brands 테이블 데이터 삽입 시작 ===");

		String csvFilePath = "brands_data.csv";

		try (Connection conn = DriverManager.getConnection(
			TableSpecificBulkInsert.JDBC_URL,
			TableSpecificBulkInsert.USERNAME,
			TableSpecificBulkInsert.PASSWORD)) {

			// 1. 테이블 준비
			prepareBrandsTable(conn);

			// 2. CSV 생성
			generateBrandsCSV(csvFilePath, recordCount);

			// 3. 데이터 삽입
			insertBrandsData(conn, csvFilePath, recordCount);

			// 4. 결과 확인
			verifyBrandsData(conn);

			// 5. 정리
			new File(csvFilePath).delete();

		} catch (Exception e) {
			System.err.println("brands 테이블 삽입 실패: " + e.getMessage());
			throw e;
		}
	}

	private static void prepareBrandsTable(Connection conn) throws SQLException {
		try (Statement stmt = conn.createStatement()) {
			System.out.println("📋 brands 테이블 준비 중...");

			// 테이블 생성 (없는 경우)
			String createTableSQL = """
				CREATE TABLE IF NOT EXISTS brands (
				    id BIGINT AUTO_INCREMENT PRIMARY KEY,
				    name VARCHAR(100) NOT NULL,
				    description VARCHAR(500) NULL,
				    logo_url VARCHAR(500) NULL,
				    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
				    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
				    deleted_at TIMESTAMP NULL
				) ENGINE=InnoDB
				""";

			stmt.execute(createTableSQL);
			System.out.println("✅ brands 테이블 준비 완료");
		}
	}

	private static void generateBrandsCSV(String filePath, int recordCount) throws IOException {
		System.out.println("📝 brands CSV 파일 생성 중...");

		Random random = new Random();
		Set<String> usedNames = new HashSet<>(); // 중복 방지
		long startTime = System.currentTimeMillis();

		try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
			for (int i = 0; i < recordCount; i++) {
				String brandName;

				// 고유한 브랜드명 생성
				do {
					if (i < BRAND_NAMES.length) {
						brandName = BRAND_NAMES[i];
					} else {
						String baseName = BRAND_NAMES[random.nextInt(BRAND_NAMES.length)];
						brandName = baseName + "_" + (i - BRAND_NAMES.length + 1);
					}
				} while (usedNames.contains(brandName));

				usedNames.add(brandName);
				java.sql.Timestamp currentTimestamp = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());
				writer.printf("%s,%s,%s%n", brandName, currentTimestamp, currentTimestamp);

				if (i > 0 && i % 100000 == 0) {
					System.out.printf("  진행률: %.1f%% (%,d건)%n", (i * 100.0) / recordCount, i);
				}
			}
		}

		long endTime = System.currentTimeMillis();
		System.out.printf("✅ brands CSV 생성 완료 - %,d건, %.1f초%n",
			recordCount, (endTime - startTime) / 1000.0);
	}

	private static void insertBrandsData(Connection conn, String csvFilePath, int recordCount) throws SQLException {
		try {
			// LOAD DATA LOCAL INFILE 시도
			loadBrandsDataLocalInfile(conn, csvFilePath);

		} catch (SQLException e) {
			System.out.println("LOAD DATA 실패, Batch Insert 사용: " + e.getMessage());
			batchInsertBrands(conn, recordCount);
		}
	}

	private static void loadBrandsDataLocalInfile(Connection conn, String csvFilePath) throws SQLException {
		String sql = String.format("""
			LOAD DATA LOCAL INFILE '%s' 
			INTO TABLE brands 
			FIELDS TERMINATED BY ',' 
			LINES TERMINATED BY '\\n'
			(name, created_at, updated_at)
			""", csvFilePath);

		long startTime = System.currentTimeMillis();

		try (Statement stmt = conn.createStatement()) {
			conn.setAutoCommit(false);
			int rowsAffected = stmt.executeUpdate(sql);
			conn.commit();

			long endTime = System.currentTimeMillis();
			System.out.printf("✅ brands LOAD DATA 완료 - %,d건, %.1f초%n",
				rowsAffected, (endTime - startTime) / 1000.0);
		}
	}

	private static void batchInsertBrands(Connection conn, int recordCount) throws SQLException {
		String sql = "INSERT INTO brands (name, created_at, updated_at) VALUES (?, ?, ?)";
		int batchSize = 10000;
		Random random = new Random();

		long startTime = System.currentTimeMillis();
		conn.setAutoCommit(false);

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			for (int i = 0; i < recordCount; i++) {
				String brandName = BRAND_NAMES[random.nextInt(BRAND_NAMES.length)] + "_" + i;
				java.sql.Timestamp currentTimestamp = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());
				pstmt.setString(1, brandName);
				pstmt.setTimestamp(2, currentTimestamp);
				pstmt.setTimestamp(3, currentTimestamp);
				pstmt.addBatch();

				if (i > 0 && i % batchSize == 0) {
					pstmt.executeBatch();
					pstmt.clearBatch();
					System.out.printf("  진행률: %.1f%%\n", (i * 100.0) / recordCount);
				}
			}

			pstmt.executeBatch();
			conn.commit();

			long endTime = System.currentTimeMillis();
			System.out.printf("✅ brands Batch Insert 완료 - %,d건, %.1f초%n",
				recordCount, (endTime - startTime) / 1000.0);
		}
	}

	private static void verifyBrandsData(Connection conn) throws SQLException {
		try (Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM brands");
			if (rs.next()) {
				System.out.printf("📊 brands 총 레코드: %,d건%n", rs.getLong(1));
			}

			rs = stmt.executeQuery("SELECT id, name FROM brands ORDER BY id LIMIT 5");
			System.out.println("📋 brands 샘플 데이터:");
			while (rs.next()) {
				System.out.printf("  ID: %d, 브랜드: %s%n", rs.getLong(1), rs.getString(2));
			}
		}
	}
}
