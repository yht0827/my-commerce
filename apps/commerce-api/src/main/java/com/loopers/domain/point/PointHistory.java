package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.UserId;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "point_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseEntity {

	@Embedded
	private UserId userId;

	@Embedded
	@AttributeOverride(name = "balance", column = @Column(name = "amount", nullable = false))
	private Balance amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private PointHistoryType type;

	@Column(name = "description", length = 200)
	private String description;

	@Builder(builderMethodName = "create")
	public PointHistory(final UserId userId, final Balance amount, final PointHistoryType type,
		final String description) {
		this.userId = userId;
		this.amount = amount;
		this.type = type;
		this.description = description;
	}

	public static PointHistory charge(final UserId userId, final Balance amount) {
		return PointHistory.create()
			.userId(userId)
			.amount(amount)
			.type(PointHistoryType.CHARGE)
			.description("포인트 충전")
			.build();
	}
}
