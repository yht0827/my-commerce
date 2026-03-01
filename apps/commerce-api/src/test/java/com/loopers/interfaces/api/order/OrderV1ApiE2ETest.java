package com.loopers.interfaces.api.order;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandId;
import com.loopers.domain.brand.BrandName;
import com.loopers.domain.common.Price;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.point.Balance;
import com.loopers.domain.point.Point;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductName;
import com.loopers.domain.user.Birthday;
import com.loopers.domain.user.Email;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserId;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.order.OrderHistoryJpaRepository;
import com.loopers.infrastructure.order.OrderItemJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.payment.PaymentJpaRepository;
import com.loopers.infrastructure.point.PointHistoryJpaRepository;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.common.ApiResponse;

@DisplayName("주문 API E2E 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest {

	private static final String TEST_USER_ID = "orderuser1";
	private static final BigDecimal INITIAL_POINT_BALANCE = BigDecimal.valueOf(1_000_000L);

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Autowired
	private OrderJpaRepository orderJpaRepository;

	@Autowired
	private OrderItemJpaRepository orderItemJpaRepository;

	@Autowired
	private PaymentJpaRepository paymentJpaRepository;

	@Autowired
	private OrderHistoryJpaRepository orderHistoryJpaRepository;

	@Autowired
	private PointJpaRepository pointJpaRepository;

	@Autowired
	private PointHistoryJpaRepository pointHistoryJpaRepository;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Autowired
	private BrandJpaRepository brandJpaRepository;

	@Autowired
	private ProductJpaRepository productJpaRepository;

	private Long productId;

	@BeforeEach
	void setUp() {
		User user = User.create()
			.userId(UserId.of(TEST_USER_ID))
			.email(Email.of("orderuser1@test.com"))
			.birthday(Birthday.of("1999-01-01"))
			.gender(Gender.of("MALE"))
			.build();
		userJpaRepository.saveAndFlush(user);
		pointJpaRepository.saveAndFlush(Point.create().userId(UserId.of(TEST_USER_ID)).balance(Balance.of(INITIAL_POINT_BALANCE)).build());

		Brand brand = brandJpaRepository.saveAndFlush(new Brand(new BrandName("order-brand")));
		Product product = productJpaRepository.saveAndFlush(
			Product.builder()
				.brandId(BrandId.of(brand.getId()))
				.name(new ProductName("order-product"))
				.price(new Price(5000L))
				.quantity(new Quantity(10L))
				.build()
		);
		productId = product.getId();
	}

	@AfterEach
	void tearDown() {
		paymentJpaRepository.deleteAllInBatch();
		orderHistoryJpaRepository.deleteAllInBatch();
		orderItemJpaRepository.deleteAllInBatch();
		orderJpaRepository.deleteAllInBatch();
		pointHistoryJpaRepository.deleteAllInBatch();
		pointJpaRepository.deleteAllInBatch();
		productJpaRepository.deleteAllInBatch();
		brandJpaRepository.deleteAllInBatch();
		userJpaRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("주문 생성 후 목록/상세 조회가 가능하다")
	void createThenGetOrdersAndDetail() {
		OrderDto.V1.OrderRequest request = new OrderDto.V1.OrderRequest(
			List.of(new OrderDto.V1.OrderItemRequest(productId, 2L)),
			null,
			CardType.KB,
			"1111-2222-3333-4444",
			"https://callback.test/orders"
		);

		ResponseEntity<ApiResponse<OrderDto.V1.OrderResponse>> createResponse = createOrder(request, null);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(createResponse.getBody()).isNotNull();
		OrderDto.V1.OrderResponse created = createResponse.getBody().data();
		assertThat(created).isNotNull();
		assertThat(created.orderId()).isNotBlank();
		assertThat(created.userId()).isEqualTo(TEST_USER_ID);
		assertThat(created.totalPrice()).isEqualTo(10000L);

		ResponseEntity<ApiResponse<List<OrderDto.V1.OrderResponse>>> listResponse = getOrders();
		assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(listResponse.getBody()).isNotNull();
		List<OrderDto.V1.OrderResponse> orders = listResponse.getBody().data();
		assertThat(orders).isNotEmpty();
		assertThat(orders).extracting(OrderDto.V1.OrderResponse::orderId).contains(created.orderId());

		ResponseEntity<ApiResponse<OrderDto.V1.OrderResponse>> detailResponse = getOrder(created.orderId());
		assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(detailResponse.getBody()).isNotNull();
		OrderDto.V1.OrderResponse detail = detailResponse.getBody().data();
		assertThat(detail).isNotNull();
		assertThat(detail.orderId()).isEqualTo(created.orderId());
		assertThat(detail.userId()).isEqualTo(TEST_USER_ID);
		assertThat(detail.totalPrice()).isEqualTo(10000L);
	}

	@Test
	@DisplayName("같은 멱등성 키로 요청하면 동일한 주문을 반환한다")
	void createOrderWithSameIdempotencyKeyReturnsSameOrder() {
		OrderDto.V1.OrderRequest request = new OrderDto.V1.OrderRequest(
			List.of(new OrderDto.V1.OrderItemRequest(productId, 1L)),
			null,
			CardType.KB,
			"1111-2222-3333-4444",
			"https://callback.test/orders"
		);

		ResponseEntity<ApiResponse<OrderDto.V1.OrderResponse>> firstResponse = createOrder(request, "idem-key-001");
		ResponseEntity<ApiResponse<OrderDto.V1.OrderResponse>> secondResponse = createOrder(request, "idem-key-001");

		assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(firstResponse.getBody()).isNotNull();
		assertThat(secondResponse.getBody()).isNotNull();

		OrderDto.V1.OrderResponse firstOrder = firstResponse.getBody().data();
		OrderDto.V1.OrderResponse secondOrder = secondResponse.getBody().data();

		assertThat(firstOrder.orderId()).isEqualTo(secondOrder.orderId());

		ResponseEntity<ApiResponse<List<OrderDto.V1.OrderResponse>>> listResponse = getOrders();
		assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(listResponse.getBody()).isNotNull();
		assertThat(listResponse.getBody().data()).hasSize(1);
	}

	private ResponseEntity<ApiResponse<OrderDto.V1.OrderResponse>> createOrder(final OrderDto.V1.OrderRequest request,
		final String idempotencyKey) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-USER-ID", TEST_USER_ID);
		if (idempotencyKey != null) {
			headers.add("X-IDEMPOTENCY-KEY", idempotencyKey);
		}
		HttpEntity<OrderDto.V1.OrderRequest> requestEntity = new HttpEntity<>(request, headers);

		ParameterizedTypeReference<ApiResponse<OrderDto.V1.OrderResponse>> responseType = new ParameterizedTypeReference<>() {
		};
		return testRestTemplate.exchange("/api/v1/orders", HttpMethod.POST, requestEntity, responseType);
	}

	private ResponseEntity<ApiResponse<List<OrderDto.V1.OrderResponse>>> getOrders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-USER-ID", TEST_USER_ID);
		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		ParameterizedTypeReference<ApiResponse<List<OrderDto.V1.OrderResponse>>> responseType = new ParameterizedTypeReference<>() {
		};
		return testRestTemplate.exchange("/api/v1/orders", HttpMethod.GET, requestEntity, responseType);
	}

	private ResponseEntity<ApiResponse<OrderDto.V1.OrderResponse>> getOrder(final String orderId) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-USER-ID", TEST_USER_ID);
		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		ParameterizedTypeReference<ApiResponse<OrderDto.V1.OrderResponse>> responseType = new ParameterizedTypeReference<>() {
		};
		return testRestTemplate.exchange("/api/v1/orders/" + orderId, HttpMethod.GET, requestEntity, responseType);
	}
}
