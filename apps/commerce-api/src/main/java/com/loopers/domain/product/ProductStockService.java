package com.loopers.domain.product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.loopers.config.lock.LockProperties;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.order.OrderData;
import com.loopers.domain.order.OrderItem;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorMessage;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductStockService {

	private final ProductRepository productRepository;
	private final RedissonClient redissonClient;
	private final LockProperties lockProperties;

	@Transactional
	public void restoreStock(final List<OrderItem> orderItems) {
		if (orderItems == null || orderItems.isEmpty()) {
			return;
		}

		Map<Long, Quantity> quantityByProductId = new HashMap<>();
		for (OrderItem orderItem : orderItems) {
			quantityByProductId.merge(
				orderItem.getProductId().getProductId(),
				orderItem.getQuantity(),
				Quantity::add
			);
		}

		quantityByProductId.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.forEach(entry -> {
				ProductId productId = ProductId.of(entry.getKey());
				Product product = findProductWithPessimisticLock(productId);
				product.restore(entry.getValue());
			});
	}

	@Transactional
	public List<ProductData.StockQuantityChanged> deductStock(final List<OrderData.OrderItemData> orderItems) {
		if (orderItems == null || orderItems.isEmpty()) {
			return List.of();
		}

		List<ProductData.StockQuantityChanged> quantityChanges = new ArrayList<>();

		for (StockDeductionItem item : normalizeOrderItemsForStockDeduction(orderItems)) {
			quantityChanges.add(deductStockItem(item));
		}

		return quantityChanges;
	}

	private ProductData.StockQuantityChanged deductStockItem(final StockDeductionItem item) {
		return switch (lockProperties.mode()) {
			case PESSIMISTIC -> deductWithPessimistic(item);
			case OPTIMISTIC -> deductWithOptimistic(item);
			case DISTRIBUTED -> deductWithDistributed(item);
			case DOUBLE_DEFENSE -> deductWithDoubleDefense(item);
		};
	}

	private ProductData.StockQuantityChanged deductWithPessimistic(final StockDeductionItem item) {
		Product product = findProductWithPessimisticLock(item.productId());
		return doDeduct(product, item);
	}

	private ProductData.StockQuantityChanged deductWithOptimistic(final StockDeductionItem item) {
		int maxRetry = lockProperties.optimisticRetryCount();
		for (int attempt = 0; attempt < maxRetry; attempt++) {
			try {
				Product product = findProductWithOptimisticLock(item.productId());
				return doDeduct(product, item);
			} catch (ObjectOptimisticLockingFailureException e) {
				log.debug("Optimistic lock conflict on stock deduction, attempt={}/{}", attempt + 1, maxRetry);
				if (attempt == maxRetry - 1) {
					throw new CoreException(ErrorType.CONFLICT, ErrorMessage.STOCK_CONFLICT.format(item.productId().getProductId()));
				}
			}
		}
		throw new CoreException(ErrorType.CONFLICT, ErrorMessage.STOCK_CONFLICT.format(item.productId().getProductId()));
	}

	private ProductData.StockQuantityChanged deductWithDistributed(final StockDeductionItem item) {
		String lockKey = "lock:stock:" + item.productId().getProductId();
		RLock lock = redissonClient.getLock(lockKey);
		lock.lock(10, TimeUnit.SECONDS);
		registerUnlockAfterCommit(lock);
		Product product = findProduct(item.productId());
		return doDeduct(product, item);
	}

	private ProductData.StockQuantityChanged deductWithDoubleDefense(final StockDeductionItem item) {
		String lockKey = "lock:stock:" + item.productId().getProductId();
		RLock lock = redissonClient.getLock(lockKey);
		lock.lock(10, TimeUnit.SECONDS);
		registerUnlockAfterCommit(lock);
		Product product = findProductWithOptimisticLock(item.productId());
		return doDeduct(product, item);
	}

	private ProductData.StockQuantityChanged doDeduct(final Product product, final StockDeductionItem item) {
		long oldQuantity = product.getQuantity().getQuantity();
		product.deduct(item.quantity());
		return new ProductData.StockQuantityChanged(
			item.productId().getProductId(),
			oldQuantity,
			product.getQuantity().getQuantity()
		);
	}

	private void registerUnlockAfterCommit(final RLock lock) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (lock.isHeldByCurrentThread()) {
					lock.unlock();
				}
			}
		});
	}

	private Product findProduct(final ProductId productId) {
		return productRepository.findEntityById(productId)
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, ErrorMessage.PRODUCT_NOT_FOUND.format(productId.getProductId())));
	}

	private Product findProductWithPessimisticLock(final ProductId productId) {
		return productRepository.findByIdWithPessimisticLock(productId)
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, ErrorMessage.PRODUCT_NOT_FOUND.format(productId.getProductId())));
	}

	private Product findProductWithOptimisticLock(final ProductId productId) {
		return productRepository.findByIdWithOptimisticLock(productId)
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, ErrorMessage.PRODUCT_NOT_FOUND.format(productId.getProductId())));
	}

	private List<StockDeductionItem> normalizeOrderItemsForStockDeduction(final List<OrderData.OrderItemData> orderItems) {
		Map<Long, StockDeductionItem> mergedItemsByProductId = new HashMap<>();

		for (OrderData.OrderItemData orderItem : orderItems) {
			if (orderItem == null || orderItem.productId() == null || orderItem.quantity() == null) {
				continue;
			}

			ProductId productId = ProductId.of(orderItem.productId());
			StockDeductionItem newItem = new StockDeductionItem(productId, new Quantity(orderItem.quantity()));

			mergedItemsByProductId.merge(
				productId.getProductId(),
				newItem,
				(existingItem, incomingItem) -> new StockDeductionItem(
					existingItem.productId(),
					existingItem.quantity().add(incomingItem.quantity())
				)
			);
		}

		return mergedItemsByProductId.values()
			.stream()
			.sorted(Comparator.comparingLong(item -> item.productId().getProductId()))
			.toList();
	}

	private record StockDeductionItem(ProductId productId, Quantity quantity) {
	}
}
