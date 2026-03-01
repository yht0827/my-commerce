package com.loopers.support.redis;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import com.loopers.domain.user.Birthday;
import com.loopers.domain.user.Email;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserId;
import com.loopers.utils.RedisCleanUp;

@SpringBootTest
public class RedisTest {

	private final RedisTemplate<String, Object> redisTemplate;
	private final RedisCleanUp redisCleanUp;

	@Autowired
	public RedisTest(RedisTemplate<String, Object> redisTemplate, RedisCleanUp redisCleanUp) {
		this.redisTemplate = redisTemplate;
		this.redisCleanUp = redisCleanUp;
	}

	@BeforeEach
	public void setUp() {
		redisCleanUp.truncateAll();
	}

	@Test
	@DisplayName("Redis 연결 테스트")
	public void redisConnectionTest() {
		redisTemplate.opsForValue().set("connection-test", "success");
		String result = (String)redisTemplate.opsForValue().get("connection-test");

		assertThat(result).isEqualTo("success");
	}

	@Test
	@DisplayName("JSON 직렬화 테스트 - User 객체를 Redis에 저장")
	public void jsonObjectSerializationTest() {
		User user = User.create()
			.userId(new UserId("testuser1"))
			.email(new Email("hong@example.com"))
			.birthday(new Birthday("1990-01-01"))
			.gender(Gender.MALE)
			.build();

		redisTemplate.opsForValue().set("user:1", user);

		Object storedUser = redisTemplate.opsForValue().get("user:1");
		assertThat(storedUser).isNotNull();
		assertThat(storedUser).isInstanceOf(User.class);
	}

	@Test
	@DisplayName("JSON 역직렬화 테스트 - Redis에서 User 객체 조회")
	public void jsonObjectDeserializationTest() {
		User originalUser = User.create()
			.userId(new UserId("testuser2"))
			.email(new Email("hong@example.com"))
			.birthday(new Birthday("1990-01-01"))
			.gender(Gender.MALE)
			.build();

		redisTemplate.opsForValue().set("user:deserialize", originalUser);

		// When: Redis에서 객체를 조회
		Object result = redisTemplate.opsForValue().get("user:deserialize");

		// Then: 역직렬화된 객체 검증
		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(User.class);

		if (result instanceof User user) {
			assertThat(user.getEmail().getEmail()).isEqualTo("hong@example.com");
			assertThat(user.getUserId().getUserId()).isEqualTo("testuser2");
			assertThat(user.getBirthday().getBirthday()).isEqualTo("1990-01-01");
			assertThat(user.getGender().name()).isEqualTo("MALE");
		}
	}

	@Test
	@DisplayName("JSON Hash 연산 테스트 - Hash 구조에서 User 객체 저장/조회")
	public void jsonHashOperationTest() {
		User user1 = User.create()
			.userId(new UserId("testuser1"))
			.email(new Email("hong@example.com"))
			.birthday(new Birthday("1990-01-01"))
			.gender(Gender.MALE)
			.build();

		User user2 = User.create()
			.userId(new UserId("testuser2"))
			.email(new Email("lee@example.com"))
			.birthday(new Birthday("1988-03-20"))
			.gender(Gender.FEMALE)
			.build();

		redisTemplate.opsForHash().put("users", "user1", user1);
		redisTemplate.opsForHash().put("users", "user2", user2);

		Object retrievedUser1 = redisTemplate.opsForHash().get("users", "user1");
		Object retrievedUser2 = redisTemplate.opsForHash().get("users", "user2");

		assertThat(retrievedUser1).isNotNull().isInstanceOf(User.class);
		assertThat(retrievedUser2).isNotNull().isInstanceOf(User.class);

		User retrievedUserObj1 = (User)retrievedUser1;
		User retrievedUserObj2 = (User)retrievedUser2;

		assertThat(retrievedUserObj1.getUserId().getUserId()).isEqualTo("testuser1");
		assertThat(retrievedUserObj2.getUserId().getUserId()).isEqualTo("testuser2");
	}

}
