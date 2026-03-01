package com.loopers.application.brand;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.brand.BrandId;
import com.loopers.domain.brand.BrandInfo;
import com.loopers.domain.brand.BrandService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandApplicationService {
	private final BrandService brandService;

	public BrandResult getBrandById(final GetBrandQuery query) {
		BrandInfo brandInfo = brandService.getBrandById(BrandId.of(query.brandId()));
		return BrandResult.from(brandInfo);
	}
}
