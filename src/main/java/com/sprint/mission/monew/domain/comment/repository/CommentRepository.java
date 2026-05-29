package com.sprint.mission.monew.domain.comment.repository;

import com.sprint.mission.monew.domain.comment.entity.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  // 일단 빈 메서드로 추가
  @Query("SELECT c FROM Comment c WHERE 1=0")
  List<Comment> findTop10RecentCommentsByUserId(@Param("userId") UUID userId);
}
