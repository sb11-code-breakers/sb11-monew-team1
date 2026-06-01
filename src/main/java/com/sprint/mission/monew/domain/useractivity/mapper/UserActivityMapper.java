package com.sprint.mission.monew.domain.useractivity.mapper;

import com.sprint.mission.monew.domain.article.entity.ArticleView;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.entity.CommentLike;
import com.sprint.mission.monew.domain.interest.entity.InterestKeyword;
import com.sprint.mission.monew.domain.interest.entity.Subscription;
import com.sprint.mission.monew.domain.useractivity.activityresponse.ArticleViewActivityResponse;
import com.sprint.mission.monew.domain.useractivity.activityresponse.CommentActivityResponse;
import com.sprint.mission.monew.domain.useractivity.activityresponse.CommentLikeActivityResponse;
import com.sprint.mission.monew.domain.useractivity.activityresponse.SubscriptionActivityResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserActivityMapper {

  default List<String> mapInterestKeywords(Subscription subscription) {
    return subscription.getInterest().getKeywords().stream()
        .map(InterestKeyword::getKeyword)
        .toList();
  }

  @Mapping(target = "interestId", source = "interest.id")
  @Mapping(target = "interestName", source = "interest.name")
  @Mapping(target = "interestKeywords", expression = "java(mapInterestKeywords(subscription))")
  @Mapping(target = "interestSubscriberCount", source = "interest.subscriberCount")
  SubscriptionActivityResponse toSubscriptionDto(Subscription subscription);

  @Mapping(target = "articleId", source = "article.id")
  @Mapping(target = "articleTitle", source = "article.title")
  @Mapping(target = "userId", expression = "java(comment.getUser() != null ? comment.getUser().getId() : null)")
  @Mapping(target = "userNickname", expression = "java(comment.getUser() != null ? comment.getUser().getNickname() : \"알 수 없음\")")
  CommentActivityResponse toCommentDto(Comment comment);

  @Mapping(target = "commentId", source = "comment.id")
  @Mapping(target = "articleId", source = "comment.article.id")
  @Mapping(target = "articleTitle", source = "comment.article.title")
  @Mapping(target = "commentUserId", expression = "java(commentLike.getComment().getUser() != null ? commentLike.getComment().getUser().getId() : null)")
  @Mapping(target = "commentUserNickname", expression = "java(commentLike.getComment().getUser() != null ? commentLike.getComment().getUser().getNickname() : \"알 수 없음\")")
  @Mapping(target = "commentContent", source = "comment.content")
  @Mapping(target = "commentLikeCount", source = "comment.likeCount")
  @Mapping(target = "commentCreatedAt", source = "comment.createdAt")
  CommentLikeActivityResponse toCommentLikeDto(CommentLike commentLike);

  @Mapping(target = "viewedBy", source = "userId")
  @Mapping(target = "articleId", source = "article.id")
  @Mapping(target = "source", expression = "java(articleView.getArticle().getSource().name())")
  @Mapping(target = "sourceUrl", source = "article.sourceUrl")
  @Mapping(target = "articleTitle", source = "article.title")
  @Mapping(target = "articlePublishedDate", source = "article.publishDate")
  @Mapping(target = "articleSummary", source = "article.summary")
  @Mapping(target = "articleCommentCount", source = "article.commentCount")
  @Mapping(target = "articleViewCount", source = "article.viewCount")
  ArticleViewActivityResponse toArticleViewDto(ArticleView articleView);
}