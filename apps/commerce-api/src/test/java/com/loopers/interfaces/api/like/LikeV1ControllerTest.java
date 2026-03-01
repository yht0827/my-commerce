package com.loopers.interfaces.api.like;

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
import com.loopers.application.like.LikeApplicationService;
import com.loopers.application.like.LikeResult;

@ExtendWith(MockitoExtension.class)
@DisplayName("LikeV1Controller 테스트")
class LikeV1ControllerTest {

	private static final String USER_ID = "user1";

	@Mock
	private LikeApplicationService likeApplicationService;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		LikeV1Controller controller = new LikeV1Controller(likeApplicationService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
		objectMapper = new ObjectMapper();
	}

	@Test
	@DisplayName("POST /api/v1/like/products/{productId} - 좋아요 응답을 반환한다")
	void likeProduct_returnsLikeResponse() throws Exception {
		// arrange
		LikeResult likeResult = new LikeResult(USER_ID, 100L);
		when(likeApplicationService.likeProduct(any())).thenReturn(likeResult);

		// act & assert
		mockMvc.perform(post("/api/v1/like/products/{productId}", 100L)
				.header("userId", USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meta.result").value("SUCCESS"))
			.andExpect(jsonPath("$.data.userId").value(USER_ID))
			.andExpect(jsonPath("$.data.productId").value(100L));

		verify(likeApplicationService, times(1)).likeProduct(any());
	}

	@Test
	@DisplayName("DELETE /api/v1/like/products/{productId} - 좋아요 취소 성공 응답을 반환한다")
	void unlikeProduct_returnsSuccessResponse() throws Exception {
		// arrange
		doNothing().when(likeApplicationService).unlikeProduct(any());

		// act & assert
		mockMvc.perform(delete("/api/v1/like/products/{productId}", 100L)
				.header("userId", USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meta.result").value("SUCCESS"));

		verify(likeApplicationService, times(1)).unlikeProduct(any());
	}

	@Test
	@DisplayName("GET /api/v1/like/products - 좋아요한 상품 목록을 반환한다")
	void getLikedProductList_returnsLikeList() throws Exception {
		// arrange
		List<LikeResult> likeResults = List.of(
			new LikeResult(USER_ID, 100L),
			new LikeResult(USER_ID, 200L)
		);
		when(likeApplicationService.getLikedProductList(any())).thenReturn(likeResults);

		// act & assert
		mockMvc.perform(get("/api/v1/like/products")
				.header("userId", USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meta.result").value("SUCCESS"))
			.andExpect(jsonPath("$.data[0].userId").value(USER_ID))
			.andExpect(jsonPath("$.data[0].productId").value(100L))
			.andExpect(jsonPath("$.data[1].productId").value(200L));

		verify(likeApplicationService, times(1)).getLikedProductList(any());
	}
}
