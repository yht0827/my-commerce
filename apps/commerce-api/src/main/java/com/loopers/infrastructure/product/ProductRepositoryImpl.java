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
	public Optional<Product> findByIdWithPessimisticLock(final ProductId id) {
		return productJpaRepository.findByIdWithPessimisticLock(id.getProductId());
	}

	@Override
	public Optional<Product> findByIdWithOptimisticLock(final ProductId id) {
		return productJpaRepository.findByIdWithOptimisticLock(id.getProductId());
	}

	@Override
	public Page<ProductInfo> getProductList(final BrandId brandId, final Pageable pageable) {
		return getProductListWithCoveringIndex(brandId, pageable);
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

	// 커버링 인덱스를 사용한 최적화된 상품 목록 조회
	private Page<ProductInfo> getProductListWithCoveringIndex(final BrandId brandId, final Pageable pageable) {
		// 커버링 인덱스로 ID만 조회
		List<Long> productIds = getProductIdsWithCoveringIndex(brandId, pageable);

		// 카운트 쿼리
		JPAQuery<Long> countQuery = createCountQuery(brandId);

		if (productIds.isEmpty()) {
			return PageableExecutionUtils.getPage(List.of(), pageable, countQuery::fetchOne);
		}

		// ProductInfo로 JOIN 조회 (정렬 순서 보장)
		List<ProductInfo> productInfos = getProductInfosByIds(productIds);

		return PageableExecutionUtils.getPage(productInfos, pageable, countQuery::fetchOne);
	}

	private List<Long> getProductIdsWithCoveringIndex(final BrandId brandId, final Pageable pageable) {
		JPAQuery<Long> idQuery = jpaQueryFactory.select(product.id).from(product);

		if (brandId != null) {
			idQuery.where(product.brandId.eq(brandId));
		}

		boolean requiresLikeJoin = pageable.getSort().stream()
			.map(Sort.Order::getProperty)
			.anyMatch(SORT_PROPERTY_LIKES_COUNT::equals);
		if (requiresLikeJoin) {
			idQuery.leftJoin(productAggregate)
				.on(productAggregate.productId.productId.eq(product.id));
		}

		applyOrderByForIdQuery(idQuery, pageable);

		return idQuery.offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
	}

	private JPAQuery<Long> createCountQuery(final BrandId brandId) {
		JPAQuery<Long> countQuery = jpaQueryFactory.select(product.count()).from(product);

		if (brandId != null) {
			countQuery.where(product.brandId.eq(brandId));
		}

		return countQuery;
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
					productAggregate.likeCount.likeCount.coalesce(0L)
				)
			)
			.from(product)
			.leftJoin(brand).on(product.brandId.brandId.eq(brand.id))
			.leftJoin(productAggregate).on(productAggregate.productId.productId.eq(product.id));
	}

	private void applyOrderByForIdQuery(final JPAQuery<Long> query, final Pageable pageable) {
		pageable.getSort().stream().map(this::createOrderSpecifierForIdQuery).forEach(query::orderBy);
	}

	private OrderSpecifier<?> createOrderSpecifierForIdQuery(final Sort.Order order) {
		Order direction = order.isAscending() ? Order.ASC : Order.DESC;

		return switch (order.getProperty()) {
			case SORT_PROPERTY_PRICE -> new OrderSpecifier<>(direction, product.price.price);
			case SORT_PROPERTY_LIKES_COUNT -> new OrderSpecifier<>(direction, productAggregate.likeCount.likeCount);
			default -> new OrderSpecifier<>(direction, product.createdAt);
		};
	}

}
