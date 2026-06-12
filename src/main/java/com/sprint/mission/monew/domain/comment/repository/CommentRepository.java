package com.sprint.mission.monew.domain.comment.repository;

import com.sprint.mission.monew.batch.comment.cleanup.dto.CommentCleanupItem;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.repository.querydsl.CommentCustomRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentCustomRepository {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Comment c set c.likeCount = c.likeCount + 1
            where c.id = :commentId
      """)
  void increaseLikeCount(UUID commentId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Comment c set c.likeCount = c.likeCount - 1
            where c.id = :commentId and c.likeCount > 0
      """)
  void decreaseLikeCount(UUID commentId);

  @Query("""
      SELECT new com.sprint.mission.monew.batch.comment.cleanup.dto.CommentCleanupItem(
          c.id,
          c.deletedAt
      )
      FROM Comment c
      WHERE c.deletedAt < :threshold
      AND (
          c.deletedAt > :lastDeletedAt
          OR (c.deletedAt = :lastDeletedAt AND c.id > :lastId)
      )
      ORDER BY c.deletedAt ASC, c.id ASC
      """)
  List<CommentCleanupItem> findCommentsForCleanup(
      @Param("threshold") Instant threshold,
      @Param("lastDeletedAt") Instant lastDeletedAt,
      @Param("lastId") UUID lastId,
      Pageable pageable
  );

}
