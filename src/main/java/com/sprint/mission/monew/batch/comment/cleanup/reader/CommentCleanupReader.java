package com.sprint.mission.monew.batch.comment.cleanup.reader;

import com.sprint.mission.monew.batch.comment.cleanup.dto.CommentCleanupItem;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class CommentCleanupReader implements ItemReader<CommentCleanupItem> {

  private final CommentRepository commentRepository;

  @Value("${batch.comment-cleanup.chunk-size}")
  private int chunkSize;

  private Instant threshold;
  private Instant lastDeletedAt;
  private UUID lastId;

  private Iterator<CommentCleanupItem> iterator;

  @Override
  public CommentCleanupItem read() {

    if (threshold == null) {
      threshold = Instant.now().minus(Duration.ofDays(1));
      lastDeletedAt = Instant.EPOCH;
      lastId = new UUID(0L, 0L);

      log.info("Comment Cleanup Reader 시작: threshold={}, chunkSize={}", threshold, chunkSize);
    }

    while (iterator == null || !iterator.hasNext()) {

      List<CommentCleanupItem> items =
          commentRepository.findCommentsForCleanup(
              threshold,
              lastDeletedAt,
              lastId,
              PageRequest.of(0, chunkSize)
          );

      if (items.isEmpty()) {
        return null;
      }

      iterator = items.iterator();
      log.info("Comment Cleanup Reader chunk load 완료: size={}", items.size());
    }

    CommentCleanupItem item = iterator.next();

    lastDeletedAt = item.deletedAt();
    lastId = item.id();

    return item;
  }
}