package com.loopers.interfaces.api.point;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.loopers.domain.point.Balance;
import com.loopers.domain.point.Point;
import com.loopers.domain.user.UserId;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.interfaces.api.common.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;

@DisplayName("포인트 E2E 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PointV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    private static final String TEST_USER_ID = "yht0827";
    private static final String NON_EXISTENT_USER_ID = "yht0827111";
    private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(500L);
    private static final BigDecimal CHARGE_AMOUNT = BigDecimal.valueOf(1000L);
    private static final BigDecimal POINT_BALANCE = BigDecimal.valueOf(1000L);

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/points")
    @Nested
    class Get {

        @Test
        @DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
        void getPoint_success() {
            // arrange
            Balance balance = new Balance(POINT_BALANCE);
            pointJpaRepository.save(new Point(new UserId(TEST_USER_ID), balance));

            // act
            ResponseEntity<ApiResponse<PointDto.V1.BalanceResponse>> response = getPointBalance();

            // assert
            PointDto.V1.BalanceResponse responseData = response.getBody().data();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(responseData.balance()).isEqualByComparingTo(balance.getBalance());
        }

        @Test
        @DisplayName("X-USER-ID 헤더가 없을 경우, 401 Unauthorized 응답을 반환한다.")
        void getPoint_fail_without_header() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // act
            ParameterizedTypeReference<ApiResponse<PointDto.V1.BalanceResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<PointDto.V1.BalanceResponse>> response =
                testRestTemplate.exchange("/api/v1/points", HttpMethod.GET, entity, responseType);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @DisplayName("POST /api/v1/points/charge")
    @Nested
    class Post {

        @Test
        @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
        void chargePoint_success() {
            // arrange
            Balance initialBalance = new Balance(INITIAL_BALANCE);
            pointJpaRepository.save(new Point(new UserId(TEST_USER_ID), initialBalance));

            // act
            ResponseEntity<ApiResponse<PointDto.V1.BalanceResponse>> response = chargeBalancePoint(TEST_USER_ID);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data().balance()).isEqualByComparingTo(INITIAL_BALANCE.add(CHARGE_AMOUNT));
        }

        @Test
        @DisplayName("존재하지 않는 유저로 요청할 경우, 404 Not Found 응답을 반환한다.")
        void chargePoint_fail_when_user_not_found() {
            // act
            ResponseEntity<ApiResponse<PointDto.V1.BalanceResponse>> response = chargeBalancePoint(NON_EXISTENT_USER_ID);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    private ResponseEntity<ApiResponse<PointDto.V1.BalanceResponse>> getPointBalance() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", PointV1ApiE2ETest.TEST_USER_ID);
        HttpEntity<Object> request = new HttpEntity<>(headers);

        ParameterizedTypeReference<ApiResponse<PointDto.V1.BalanceResponse>> responseType = new ParameterizedTypeReference<>() {
        };
        return testRestTemplate.exchange("/api/v1/points", HttpMethod.GET, request, responseType);
    }

    private ResponseEntity<ApiResponse<PointDto.V1.BalanceResponse>> chargeBalancePoint(String userId) {
        PointDto.V1.ChargePointRequest pointRequest = new PointDto.V1.ChargePointRequest(userId, PointV1ApiE2ETest.CHARGE_AMOUNT);
        HttpEntity<PointDto.V1.ChargePointRequest> requestEntity = new HttpEntity<>(pointRequest);

        ParameterizedTypeReference<ApiResponse<PointDto.V1.BalanceResponse>> responseType = new ParameterizedTypeReference<>() {
        };
        return testRestTemplate.exchange("/api/v1/points/charge", HttpMethod.POST, requestEntity, responseType);
    }
}
