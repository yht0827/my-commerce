package com.loopers.testcontainers;

import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@Configuration
public class MySqlTestContainersConfig {

	private static final String MYSQL_IMAGE = "mysql:8.0";
	private static final String MYSQL_DATABASE = "loopers";
	private static final String MYSQL_USER = "application";
	private static final String MYSQL_PASSWORD = "application";

    private static final MySQLContainer<?> mySqlContainer;

    static {
        mySqlContainer = new MySQLContainer<>(DockerImageName.parse(MYSQL_IMAGE))
            .withDatabaseName(MYSQL_DATABASE)
            .withUsername(MYSQL_USER)
            .withPassword(MYSQL_PASSWORD)
            .withExposedPorts(3306)
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_0900_ai_ci",
                "--skip-character-set-client-handshake"
            );
        mySqlContainer.start();

		System.setProperty("MYSQL_HOST", mySqlContainer.getHost());
		System.setProperty("MYSQL_PORT", String.valueOf(mySqlContainer.getFirstMappedPort()));
		System.setProperty("MYSQL_DATABASE", mySqlContainer.getDatabaseName());
		System.setProperty("MYSQL_USER", mySqlContainer.getUsername());
		System.setProperty("MYSQL_PWD", mySqlContainer.getPassword());
    }
}
