package com.loopers.infrastructure.product;

import static com.loopers.domain.brand.QBrand.*;
import static com.loopers.domain.product.QProduct.*;
import static com.loopers.domain.product.QProductAggregate.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.loopers.domain.brand.BrandId;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.ProductInfo;
import com.loopers.domain.product.ProductRepository;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {
	private static final String SORT_PROPERTY_PRICE = "price";
	private static final String SORT_PROPERTY_LIKES_COUNT = "likesCount";
	private static final String SORT_PROPERTY_ORDER_COUNT = "orderCount";
	private static final String SORT_PROPERTY_VIEW_COUNT = "viewCount";

	private final ProductJpaRepository productJpaRepository;
	private final JPAQueryFactory jpaQueryFactory;

	@Override
	public Optional<ProductInfo> findById(final ProductId id) {
		ProductInfo productInfo = createProductInfoQuery()
			.where(product.id.eq(id.getProductId()))
			.fetchOne();

		return Optional.ofNullable(productInfo);
	}

	@Override
	public Optional<Product> findEntityById(final ProductId id) {
		return productJpaRepository.findById(id.getProductId());
	}

	@Override
	public Optional<Product> findByIdWithPessimisticLock(final ProductId id) {
		return productJpaRepository.findByIdWithPessimisticLock(id.getProductId());
	}

	@Override
	public Optional<Product> findByIdWithOptimisticLock(final ProductId id) {
		return productJpaRepository.findByIdWithOptimisticLock(id.getProductId());
	}

	@Override
	public Page<ProductInfo> getProductList(final BrandId brandId, final Pageable pageable) {
		JPAQuery<ProductInfo> productInfoQuery = applyBrandFilter(createProductInfoQuery(), brandId);

		applyOrderBy(productInfoQuery, pageable);

		List<ProductInfo> productInfos = productInfoQuery
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		JPAQuery<Long> countQuery = createCountQuery(brandId);
		return PageableExecutionUtils.getPage(productInfos, pageable, countQuery::fetchOne);
	}

	@Override
	public Product save(final Product product) {
		return productJpaRepository.save(product);
	}

	@Override
	public List<ProductInfo> findInfosByIds(final List<ProductId> ids) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		List<Long> productIds = ids.stream()
			.map(ProductId::getProductId)
			.toList();
		return getProductInfosByIds(productIds);
	}

	private JPAQuery<Long> createCountQuery(final BrandId brandId) {
		JPAQuery<Long> countQuery = jpaQueryFactory.select(product.count()).from(product);
		return applyBrandFilter(countQuery, brandId);
	}

	private List<ProductInfo> getProductInfosByIds(final List<Long> productIds) {
		List<ProductInfo> allProductInfos = createProductInfoQuery()
			.where(product.id.in(productIds))
			.fetch();

		Map<Long, ProductInfo> productInfoById = allProductInfos.stream()
			.collect(Collectors.toMap(ProductInfo::productId, productInfo -> productInfo, (first, ignored) -> first));

		return productIds.stream()
			.map(productInfoById::get)
			.filter(Objects::nonNull)
			.toList();
	}

	private JPAQuery<ProductInfo> createProductInfoQuery() {
		return jpaQueryFactory.select(
				Projections.constructor(
					ProductInfo.class,
					product.id,
					product.name.name,
					product.price.price,
					product.quantity.quantity,
					brand.brandName.brandName,
					productAggregate.likeCount.likeCount.coalesce(0L),
					productAggregate.orderCount.orderCount.coalesce(0L),
					productAggregate.viewCount.viewCount.coalesce(0L)
				)
			)
			.from(product)
			.leftJoin(brand).on(product.brandId.brandId.eq(brand.id))
			.leftJoin(productAggregate).on(productAggregate.productId.productId.eq(product.id));
	}

	private <T extends JPAQuery<?>> T applyBrandFilter(final T query, final BrandId brandId) {
		if (brandId != null) {
			query.where(product.brandId.eq(brandId));
		}
		return query;
	}

	private void applyOrderBy(final JPAQuery<?> query, final Pageable pageable) {
		if (pageable.getSort().isUnsorted()) {
			query.orderBy(product.createdAt.desc());
			return;
		}

		pageable.getSort().stream().map(this::createOrderSpecifier).forEach(query::orderBy);
	}

	private OrderSpecifier<?> createOrderSpecifier(final Sort.Order order) {
		Order direction = order.isAscending() ? Order.ASC : Order.DESC;

		return switch (order.getProperty()) {
			case SORT_PROPERTY_PRICE -> new OrderSpecifier<>(direction, product.price.price);
			case SORT_PROPERTY_LIKES_COUNT -> new OrderSpecifier<>(direction, productAggregate.likeCount.likeCount);
			case SORT_PROPERTY_ORDER_COUNT -> new OrderSpecifier<>(direction, productAggregate.orderCount.orderCount);
			case SORT_PROPERTY_VIEW_COUNT -> new OrderSpecifier<>(direction, productAggregate.viewCount.viewCount);
			default -> new OrderSpecifier<>(direction, product.createdAt);
		};
	}

}
