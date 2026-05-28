package com.sprint.mission.monew.domain.comment.repository;

import com.sprint.mission.monew.domain.comment.entity.Comment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

}
