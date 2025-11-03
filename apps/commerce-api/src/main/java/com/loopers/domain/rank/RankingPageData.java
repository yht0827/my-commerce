package com.loopers.domain.rank;

import org.springframework.data.domain.Page;

public record RankingPageData(Page<RankingItem> page, RankingSource source) {

	public boolean isEmpty() {
		return page == null || page.isEmpty();
	}
}
