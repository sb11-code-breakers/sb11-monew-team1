package com.sprint.mission.monew.domain.comment.repository;

import com.sprint.mission.monew.domain.comment.entity.CommentLike;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

  boolean existsByUserIdAndCommentId(UUID userId, UUID commentId);

  int deleteByUserIdAndCommentId(UUID userId, UUID commentId);
}
