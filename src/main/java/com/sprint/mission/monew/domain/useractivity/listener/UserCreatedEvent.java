package com.sprint.mission.monew.domain.useractivity.listener;

import com.sprint.mission.monew.domain.useractivity.document.UserActivity;

public record UserCreatedEvent(UserActivity userActivity) {
}