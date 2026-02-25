package com.loopers.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.loopers.application.order.OrderApplicationService;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.point.Point;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.coupon.CouponJpaRepository;
import com.loopers.infrastructure.order.OrderItemJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;

@DisplayName("주문 동시성 통합 테스트")
@ActiveProfiles("test")
@SpringBootTest
public class OrderConcurrencyTest {

	@Autowired
	private OrderApplicationService orderApplicationService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderJpaRepository orderJpaRepository;

	@Autowired
	private OrderItemJpaRepository orderItemJpaRepository;

	@Autowired
	private ProductJpaRepository productJpaRepository;

	@Autowired
	private BrandJpaRepository brandRepository;

	@Autowired
	private PointJpaRepository pointJpaRepository;

	@Autowired
	private CouponJpaRepository couponJpaRepository;

	@Autowired
	private ProductRepository productRepository;

	private Product product;
	private Point point;
	private Coupon coupon;
/*
	@BeforeEach
	void setUp() {
		productJpaRepository.deleteAll();
		brandRepository.deleteAll();
		couponJpaRepository.deleteAll();
		pointJpaRepository.deleteAll();
		orderJpaRepository.deleteAll();

		Brand brand = new Brand(new BrandName("나이키"));
		Brand brand1 = brandRepository.save(brand);

		product = new Product(new BrandId(brand1.getId()), new ProductName("티셔츠"),
			new Price(1000L), new Quantity(10L));

		point = new Point(new UserId("yht0827"), new Balance(BigDecimal.valueOf(10000L)));

		long daysInPast = ThreadLocalRandom.current().nextLong(1, 31);
		ZonedDateTime randomIssuedAt = ZonedDateTime.now().minusDays(daysInPast);

		long daysInFuture = ThreadLocalRandom.current().nextLong(1, 31);
		ZonedDateTime randomExpiredAt = ZonedDateTime.now().plusDays(daysInFuture);

		coupon = new Coupon(new UserId("yht0827"), new ProductId(1L), new BrandId(1L),
			new CouponName("쿠폰1"), new DiscountValue(100L), new MaxDiscountAmount(0L), CouponType.FIXED_AMOUNT,
			new CouponIssuedAt(randomIssuedAt), new CouponUsedAt(ZonedDateTime.now()),
			new CouponExpiredAt(randomExpiredAt), CouponStatus.ACTIVE);

		couponJpaRepository.save(coupon);
		pointJpaRepository.save(point);
		productJpaRepository.save(product);
	}

	@Test
	@DisplayName("동일한 상품에 대해 여러 주문이 동시에 요청되어도, 재고가 정상적으로 차감되어야 한다")
	void createLike_concurrency_with_lock() throws InterruptedException {
		// given
		int numberOfThreads = 10;
		ExecutorService executorService = Executors.newFixedThreadPool(32);
		CountDownLatch latch = new CountDownLatch(numberOfThreads);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failCount = new AtomicInteger(0);
		Long userId = 1L;
		Long productId = 1L;

		// when
		for (int i = 0; i < numberOfThreads; i++) {
			executorService.submit(() -> {
				try {
					OrderCriteria.CreateOrder createOrder = new OrderCriteria
						.CreateOrder(userId, List.of(new OrderCriteria.CreateOrder.OrderItem(1L, 1L)), null);

					orderFacade.createOrder(createOrder);
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executorService.shutdown();

		// then
		Product product = productRepository.findById(productId)
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

		assertThat(product.getQuantity().quantity()).isZero();
		assertThat(successCount.get()).isEqualTo(numberOfThreads);
		assertThat(failCount.get()).isEqualTo(0);
	}

	@Test
	@DisplayName("서로 다른 주문을 동시에 수행해도, 포인트가 정상적으로 차감되어야 한다.")
	void createOrder_concurrency_with_points() throws InterruptedException {
		// given
		int numberOfThreads = 10;
		ExecutorService executorService = Executors.newFixedThreadPool(32);
		CountDownLatch latch = new CountDownLatch(numberOfThreads);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failCount = new AtomicInteger(0);
		Long userId = 1L;
		Long productId = 1L;

		long initialPoints = point.getBalance();
		long pointsToUsePerOrder = 1000L;

		// when
		for (int i = 0; i < numberOfThreads; i++) {
			executorService.submit(() -> {
				try {
					OrderCriteria.CreateOrder createOrder = new OrderCriteria
						.CreateOrder(userId, List.of(new OrderCriteria.CreateOrder.OrderItem(productId, 1L)), null);

					orderFacade.createOrder(createOrder);
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executorService.shutdown();

		// then
		Point resultPoint = pointJpaRepository.findByUserId(1L).orElseThrow();

		assertThat(resultPoint.getBalance()).isEqualTo(initialPoints - (successCount.get() * pointsToUsePerOrder));
		assertThat(successCount.get()).isEqualTo(numberOfThreads);
		assertThat(failCount.get()).isZero();
	}

	@Test
	@DisplayName("동일한 쿠폰으로 여러 기기에서 동시에 주문해도, 쿠폰은 단 한번만 사용되어야 한다.")
	void createOrder_concurrency_with_coupon() throws InterruptedException {
		// given
		int numberOfThreads = 10;
		ExecutorService executorService = Executors.newFixedThreadPool(32);
		CountDownLatch latch = new CountDownLatch(numberOfThreads);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failCount = new AtomicInteger(0);
		Long userId = 1L;
		Long productId = 1L;
		Long couponId = 1L;

		// when
		for (int i = 0; i < numberOfThreads; i++) {
			executorService.submit(() -> {
				try {
					OrderCriteria.CreateOrder createOrder = new OrderCriteria
						.CreateOrder(userId, List.of(new OrderCriteria.CreateOrder.OrderItem(productId, 1L)), couponId);

					orderFacade.createOrder(createOrder);
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executorService.shutdown();

		// then
		Coupon coupon = couponJpaRepository.findById(couponId)
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰이 존재하지 않습니다."));

		Order order = orderJpaRepository.findById(1L)
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다."));

		assertThat(coupon.getCouponStatus()).isEqualTo(CouponStatus.USED);
		assertThat(order.getTotalOrderPrice()).isEqualTo(new TotalOrderPrice(1000L));
		assertThat(order.getCouponDiscountAmount()).isEqualTo(new CouponDiscountAmount(100L));
		assertThat(order.getPointUsedAmount()).isEqualTo(new PointUsedAmount(900L));
		assertThat(successCount.get()).isEqualTo(1);
		assertThat(failCount.get()).isEqualTo(numberOfThreads - 1);
	}
 */
}
