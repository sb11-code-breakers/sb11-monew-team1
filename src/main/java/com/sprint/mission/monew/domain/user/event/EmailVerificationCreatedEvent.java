package com.sprint.mission.monew.domain.user.event;

public record EmailVerificationCreatedEvent(String email, String token) {
}