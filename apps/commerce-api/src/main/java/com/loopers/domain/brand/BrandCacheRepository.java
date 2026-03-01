package com.loopers.domain.brand;

import java.util.Optional;

public interface BrandCacheRepository {

	Optional<BrandInfo> findById(BrandId brandId);

	void save(BrandId brandId, BrandInfo brandInfo);
}

