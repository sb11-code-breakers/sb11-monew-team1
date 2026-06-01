package com.sprint.mission.monew.domain.comment.repository;

import com.sprint.mission.monew.domain.comment.entity.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, UUID> {


  @Query("SELECT c FROM Comment c " +
      "JOIN FETCH c.article a " +
      "WHERE c.user.id = :userId " +
      "AND c.deletedAt IS NULL " +
      "AND a.deletedAt IS NULL " +
      "ORDER BY c.createdAt DESC " +
      "LIMIT 10")
  List<Comment> findTop10RecentCommentsByUserId(@Param("userId") UUID userId);

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

}
