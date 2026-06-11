package com.sprint.mission.monew.domain.comment.repository;

import com.sprint.mission.monew.domain.comment.entity.CommentLike;
import com.sprint.mission.monew.domain.useractivity.projection.CommentLikeLiveData;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

  boolean existsByUserIdAndCommentId(UUID userId, UUID commentId);

  @Query("""
      select c1.comment.id from CommentLike c1
      where c1.user.id = :userId
      and c1.comment.id in :commentIds
      """)
  Set<UUID> findLikedCommentIds(@Param("userId") UUID userId, @Param("commentIds") List<UUID> commentIds);

  int deleteByUserIdAndCommentId(UUID userId, UUID commentId);

  @Query("SELECT cl FROM CommentLike cl " +
      "JOIN FETCH cl.comment c " +
      "JOIN FETCH c.article a " +
      "LEFT JOIN FETCH c.user u " +
      "WHERE cl.user.id = :userId " +
      "AND c.deletedAt IS NULL " +
      "AND a.deletedAt IS NULL " +
      "ORDER BY cl.createdAt DESC "
  )
  List<CommentLike> findTop10ByUserId(@Param("userId") UUID userId, Pageable pageable);

  @Query("""
      SELECT cl.id as id, c.id as commentId, u.id as commentUserId,
             u.nickname as commentUserNickname, c.content as commentContent, c.likeCount as commentLikeCount
      FROM CommentLike cl
      JOIN cl.comment c
      LEFT JOIN c.user u
      WHERE cl.user.id = :userId
      AND c.id IN :commentIds
      AND c.deletedAt IS NULL
      """)
  List<CommentLikeLiveData> findCommentLikeLiveDataByUserIdAndCommentIds(
      @Param("userId") UUID userId, @Param("commentIds") List<UUID> commentIds);
}
