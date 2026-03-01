package com.loopers.interfaces.api.ranking;

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
import com.loopers.application.ranking.RankingApplicationService;
import com.loopers.application.ranking.RankingPageResult;
import com.loopers.application.ranking.RankingProductResult;
import com.loopers.interfaces.api.common.CurrentUserIdArgumentResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankingV1Controller 테스트")
class RankingV1ControllerTest {

	private static final String USER_ID = "rankinguser1";

	@Mock
	private RankingApplicationService rankingApplicationService;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		RankingV1Controller controller = new RankingV1Controller(rankingApplicationService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.build();
		objectMapper = new ObjectMapper();
	}

	@Test
	@DisplayName("GET /api/v1/rankings - 랭킹 목록을 반환한다")
	void getRanking_returnsRankingPageResponse() throws Exception {
		// arrange
		RankingProductResult item1 = new RankingProductResult(1L, 100.0, 10L, "에어맥스 270", 129000L, 15L, "나이키", 321L);
		RankingProductResult item2 = new RankingProductResult(2L, 80.0, 20L, "런닝화 Pro", 89000L, 30L, "아디다스", 150L);
		RankingPageResult pageResult = new RankingPageResult(0, 10, 2L, 1, List.of(item1, item2));
		when(rankingApplicationService.getRanking(any())).thenReturn(pageResult);

		// act & assert
		mockMvc.perform(get("/api/v1/rankings")
				.header("X-USER-ID", USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meta.result").value("SUCCESS"))
			.andExpect(jsonPath("$.data.page").value(0))
			.andExpect(jsonPath("$.data.size").value(10))
			.andExpect(jsonPath("$.data.totalElements").value(2L))
			.andExpect(jsonPath("$.data.totalPages").value(1))
			.andExpect(jsonPath("$.data.items[0].rank").value(1L))
			.andExpect(jsonPath("$.data.items[0].productName").value("에어맥스 270"))
			.andExpect(jsonPath("$.data.items[1].rank").value(2L))
			.andExpect(jsonPath("$.data.items[1].productName").value("런닝화 Pro"));

		verify(rankingApplicationService, times(1)).getRanking(any());
	}
}
