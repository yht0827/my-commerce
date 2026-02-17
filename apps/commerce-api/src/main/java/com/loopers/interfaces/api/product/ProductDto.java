package com.loopers.interfaces.api.product;

import java.util.List;

import org.springframework.data.domain.PageRequest;

import com.loopers.application.product.GetProductListQuery;
import com.loopers.application.product.ProductDetailResult;
import com.loopers.application.product.ProductListResult;
import com.loopers.application.product.ProductSummaryResult;
import com.loopers.domain.product.ProductSortType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductDto() {

	public record V1() {

		public record ProductRequest(
			@Schema(description = "페이지 번호(0부터 시작)", example = "0")
			@PositiveOrZero Integer page,
			@Schema(description = "페이지 크기", example = "20")
			@Positive Integer size,
			@Schema(description = "브랜드 ID 필터", example = "1")
			Long brandId,
			@Schema(description = "정렬 기준(latest, price_asc, likes_desc)", example = "latest")
			String sort
		) {

			public GetProductListQuery toQuery() {
				return new GetProductListQuery(
					brandId,
					PageRequest.of(
						page != null ? page : 0,
						size != null ? size : 10,
						(sort != null ? ProductSortType.fromRequestValue(sort) : ProductSortType.LATEST).getSort()
					));
			}
		}

		public record ProductDetailResponse(
			@Schema(description = "상품 ID", example = "1")
			Long productId,
			@Schema(description = "상품명", example = "에어맥스 270")
			String productName,
			@Schema(description = "가격", example = "129000")
			Long price,
			@Schema(description = "재고 수량", example = "15")
			Long quantity,
			@Schema(description = "브랜드명", example = "나이키")
			String brandName,
			@Schema(description = "좋아요 수", example = "321")
			Long likeCount
		) {
			public static ProductDetailResponse from(final ProductDetailResult productDetailResult) {
				return new ProductDetailResponse(
					productDetailResult.productId(), productDetailResult.productName(), productDetailResult.price(),
					productDetailResult.quantity(), productDetailResult.brandName(), productDetailResult.likeCount());
			}
		}

		public record ProductSummaryResponse(
			@Schema(description = "상품 ID", example = "1")
			Long productId,
			@Schema(description = "상품명", example = "에어맥스 270")
			String productName,
			@Schema(description = "가격", example = "129000")
			Long price,
			@Schema(description = "재고 수량", example = "15")
			Long quantity,
			@Schema(description = "브랜드명", example = "나이키")
			String brandName,
			@Schema(description = "좋아요 수", example = "321")
			Long likeCount
		) {
			public static ProductSummaryResponse from(final ProductSummaryResult productSummaryResult) {
				return new ProductSummaryResponse(
					productSummaryResult.productId(),
					productSummaryResult.productName(),
					productSummaryResult.price(),
					productSummaryResult.quantity(),
					productSummaryResult.brandName(),
					productSummaryResult.likeCount()
				);
			}
		}

		public record ProductListResponse(
			@Schema(description = "상품 목록")
			List<ProductSummaryResponse> products,
			@Schema(description = "전체 페이지 수", example = "5")
			Integer totalPages,
			@Schema(description = "전체 데이터 수", example = "96")
			Long totalElements
		) {

			public static ProductListResponse from(final ProductListResult productListResult) {
				return new ProductListResponse(
					productListResult.products().stream().map(ProductSummaryResponse::from).toList(),
					productListResult.totalPages(),
					productListResult.totalElements()
				);
			}
		}

	}
}
