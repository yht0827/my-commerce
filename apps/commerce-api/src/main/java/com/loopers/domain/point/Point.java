package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.UserId;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point extends BaseEntity {

	@Embedded
	private UserId userId;

	@Embedded
	private Balance balance;

	@Builder(builderMethodName = "create")
	public Point(final UserId userId, final Balance balance) {
		this.userId = userId;
		this.balance = balance;
	}

	public void chargeBalance(final Balance balance) {
		this.balance = this.balance.charge(balance);
	}

	public void useBalance(final Balance amount) {
		this.balance = this.balance.use(amount);
	}
}
