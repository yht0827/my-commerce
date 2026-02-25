package com.loopers.domain.product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import com.loopers.domain.common.Quantity;
import com.loopers.domain.order.OrderData;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorMessage;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductStockService {

	private final ProductRepository productRepository;

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
		ProductId productId = item.productId();
		Product product = findProductWithPessimisticLock(productId);
		long oldQuantity = product.getQuantity().getQuantity();

		product.deduct(item.quantity());
		return new ProductData.StockQuantityChanged(
			productId.getProductId(),
			oldQuantity,
			product.getQuantity().getQuantity()
		);
	}

	private Product findProductWithPessimisticLock(final ProductId productId) {
		return productRepository.findByIdWithPessimisticLock(productId)
			.orElseThrow(
				() -> new CoreException(ErrorType.NOT_FOUND, ErrorMessage.PRODUCT_NOT_FOUND.format(productId.getProductId())));
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
