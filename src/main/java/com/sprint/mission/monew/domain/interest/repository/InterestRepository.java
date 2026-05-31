package com.sprint.mission.monew.domain.interest.repository;

import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.querydsl.InterestCustomRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestRepository
    extends JpaRepository<Interest, UUID>, InterestCustomRepository {

  @Query(
      """
      SELECT DISTINCT i FROM Interest i JOIN i.keywords k
       WHERE LOWER(:title) LIKE LOWER(CONCAT('%', k.keyword, '%'))
          OR LOWER(:summary) LIKE LOWER(CONCAT('%', k.keyword, '%'))
      """)
  List<Interest> findMatchingInterests(
      @Param("title") String title, @Param("summary") String summary);
}
