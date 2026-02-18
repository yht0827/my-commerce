package com.loopers.application.like;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.like.LikeInfo;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.event.ProductLikedEvent;
import com.loopers.domain.product.event.ProductUnLikedEvent;
import com.loopers.infrastructure.like.LikeEventPublisher;
import com.loopers.support.event.EventPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class LikeFacade {

	private final LikeService likeService;
	private final EventPublisher eventPublisher;
	private final LikeEventPublisher likeEventPublisher;

	public LikeResult likeProduct(final LikeCommand.LikeProduct command) {
		LikeInfo likeInfo = likeService.likeProduct(command.toData());

		ProductLikedEvent event = ProductLikedEvent.create(command.userId(), command.productId());
		eventPublisher.publish(event);
		likeEventPublisher.publishLike(event.userId(), event.productId());

		return LikeResult.from(likeInfo);
	}

	public void unlikeProduct(final LikeCommand.UnlikeProduct command) {
		likeService.unlikeProduct(command.toData());

		ProductUnLikedEvent event = ProductUnLikedEvent.create(command.userId(), command.productId());
		eventPublisher.publish(event);
		likeEventPublisher.publishUnlike(event.userId(), event.productId());
	}

	@Transactional(readOnly = true)
	public List<LikeResult> getLikedProductList(final LikeQuery.GetLikedProducts query) {
		return likeService.getLikedProductList(query.toData())
			.stream()
			.map(LikeResult::from)
			.toList();
	}

}
