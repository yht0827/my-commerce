package com.loopers.interfaces.api.brand;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.brand.BrandApplicationService;
import com.loopers.application.brand.BrandResult;

@ExtendWith(MockitoExtension.class)
@DisplayName("BrandV1Controller 테스트")
class BrandV1ControllerTest {

	@Mock
	private BrandApplicationService brandApplicationService;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		BrandV1Controller controller = new BrandV1Controller(brandApplicationService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
		objectMapper = new ObjectMapper();
	}

	@Test
	@DisplayName("GET /api/v1/brands/{brandId} - brandId에 해당하는 브랜드 정보를 반환한다")
	void getBrandById_returnsBrandDetailResponse() throws Exception {
		// arrange
		BrandResult brandResult = new BrandResult(1L, "나이키", "글로벌 스포츠 브랜드", "https://cdn.example.com/logo/nike.png");
		when(brandApplicationService.getBrandById(any())).thenReturn(brandResult);

		// act & assert
		mockMvc.perform(get("/api/v1/brands/{brandId}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meta.result").value("SUCCESS"))
			.andExpect(jsonPath("$.data.brandId").value(1L))
			.andExpect(jsonPath("$.data.brandName").value("나이키"))
			.andExpect(jsonPath("$.data.description").value("글로벌 스포츠 브랜드"))
			.andExpect(jsonPath("$.data.logoUrl").value("https://cdn.example.com/logo/nike.png"));

		verify(brandApplicationService, times(1)).getBrandById(any());
	}
}
