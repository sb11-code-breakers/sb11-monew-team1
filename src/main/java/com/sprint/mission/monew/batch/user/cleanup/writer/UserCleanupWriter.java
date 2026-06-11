package com.sprint.mission.monew.batch.user.cleanup.writer;

import com.sprint.mission.monew.batch.user.cleanup.dto.UserCleanupItem;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCleanupWriter implements ItemWriter<UserCleanupItem> {

  private final UserRepository userRepository;

  @Override
  public void write(Chunk<? extends UserCleanupItem> chunk) {

    List<UUID> ids = chunk.getItems()
        .stream()
        .map(UserCleanupItem::id)
        .toList();

    log.info("User Cleanup Writer 실행: delete size={}", ids.size());

    userRepository.deleteAllByIdInBatch(ids);

    log.info("User Cleanup Writer 완료");
  }
}