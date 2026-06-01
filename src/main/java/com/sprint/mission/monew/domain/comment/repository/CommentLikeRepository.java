package com.sprint.mission.monew.domain.comment.repository;

import com.sprint.mission.monew.domain.comment.entity.CommentLike;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

  boolean existsByUserIdAndCommentId(UUID userId, UUID commentId);

  int deleteByUserIdAndCommentId(UUID userId, UUID commentId);

  @Query("SELECT cl FROM CommentLike cl " +
      "JOIN FETCH cl.comment c " +
      "JOIN FETCH c.article a " +
      "LEFT JOIN FETCH c.user u " +
      "WHERE cl.user.id = :userId " +
      "AND c.deletedAt IS NULL " +
      "AND a.deletedAt IS NULL " +
      "ORDER BY cl.createdAt DESC " +
      "LIMIT 10")
  List<CommentLike> findTop10ByUserId(@Param("userId") UUID userId);
}
