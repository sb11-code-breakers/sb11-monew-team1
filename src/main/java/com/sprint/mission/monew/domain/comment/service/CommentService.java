package com.sprint.mission.monew.domain.comment.service;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.exception.ArticleNotFoundException;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.comment.dto.request.CommentCreateRequest;
import com.sprint.mission.monew.domain.comment.dto.response.CommentResponse;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.mapper.CommentMapper;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

  private final CommentRepository commentRepository;
  private final ArticleRepository articleRepository;
  private final UserRepository userRepository;
  private final CommentMapper commentMapper;

  @Transactional
  public CommentResponse create(CommentCreateRequest request) {

    log.debug("[COMMENT_CREATE] 댓글 생성 시작 - 뉴스 기사 ID={}, 댓글 작성자 ID={}",
        request.articleId(), request.userId());

    Article article = articleRepository.findById(request.articleId()).orElseThrow(
        () -> ArticleNotFoundException.withId(request.articleId())
    );
    User user = userRepository.findById(request.userId()).orElseThrow(
        () -> UserNotFoundException.withId(request.userId())
    );

    Comment comment = Comment.create(article, user, request.content());
    Comment savedComment = commentRepository.save(comment);

    log.info("[COMMENT_CREATE_SUCCESS] 댓글 생성 성공 - 댓글 ID={}, 뉴스 기사 ID={}, 댓글 작성자 ID={}",
        savedComment.getId(), request.articleId(), request.userId());

    return commentMapper.toResponse(savedComment, false);
  }
}
