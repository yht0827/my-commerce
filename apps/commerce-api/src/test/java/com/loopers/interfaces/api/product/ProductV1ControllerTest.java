package com.loopers.interfaces.api.product;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.product.ProductApplicationService;
import com.loopers.application.product.ProductDetailResult;
import com.loopers.application.product.ProductListResult;
import com.loopers.application.product.ProductSummaryResult;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductV1Controller 테스트")
class ProductV1ControllerTest {

	@Mock
	private ProductApplicationService productApplicationService;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		ProductV1Controller controller = new ProductV1Controller(productApplicationService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
		objectMapper = new ObjectMapper();
	}

	@Test
	@DisplayName("GET /api/v1/products - 상품 목록을 반환한다")
	void getProductList_returnsProductListResponse() throws Exception {
		// arrange
		ProductSummaryResult summaryResult = new ProductSummaryResult(1L, "에어맥스 270", 129000L, 15L, "나이키", 321L, 45L, 1234L);
		ProductListResult listResult = new ProductListResult(List.of(summaryResult), 1, 1L);
		when(productApplicationService.getProductList(any())).thenReturn(listResult);

		// act & assert
		mockMvc.perform(get("/api/v1/products"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meta.result").value("SUCCESS"))
			.andExpect(jsonPath("$.data.products[0].productId").value(1L))
			.andExpect(jsonPath("$.data.products[0].productName").value("에어맥스 270"))
			.andExpect(jsonPath("$.data.products[0].price").value(129000L))
			.andExpect(jsonPath("$.data.totalPages").value(1))
			.andExpect(jsonPath("$.data.totalElements").value(1L));

		verify(productApplicationService, times(1)).getProductList(any());
	}

	@Test
	@DisplayName("GET /api/v1/products/{productId} - 상품 상세 정보를 반환한다")
	void getProductDetail_returnsProductDetailResponse() throws Exception {
		// arrange
		ProductDetailResult detailResult = new ProductDetailResult(1L, "에어맥스 270", 129000L, 15L, "나이키", 321L, 45L, 1234L, 1L);
		when(productApplicationService.getProductDetail(any())).thenReturn(detailResult);

		// act & assert
		mockMvc.perform(get("/api/v1/products/{productId}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meta.result").value("SUCCESS"))
			.andExpect(jsonPath("$.data.productId").value(1L))
			.andExpect(jsonPath("$.data.productName").value("에어맥스 270"))
			.andExpect(jsonPath("$.data.price").value(129000L))
			.andExpect(jsonPath("$.data.quantity").value(15L))
			.andExpect(jsonPath("$.data.brandName").value("나이키"))
			.andExpect(jsonPath("$.data.likeCount").value(321L))
			.andExpect(jsonPath("$.data.orderCount").value(45L))
			.andExpect(jsonPath("$.data.viewCount").value(1234L));

		verify(productApplicationService, times(1)).getProductDetail(any());
	}
}
