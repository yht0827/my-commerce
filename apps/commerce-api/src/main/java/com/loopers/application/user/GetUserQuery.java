package com.loopers.application.user;

public record GetUserQuery(String userId) {
    public static GetUserQuery of(final String userId) {
        return new GetUserQuery(userId);
    }
}
