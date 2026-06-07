package com.sprint.mission.monew.domain.interest.entity;

import com.sprint.mission.monew.common.entity.BaseUpdatableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "interests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interest extends BaseUpdatableEntity {

  @Column(nullable = false, length = 50)
  private String name;

  @BatchSize(size = 100)
  @OneToMany(mappedBy = "interest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<InterestKeyword> keywords = new ArrayList<>();

  @Column(nullable = false)
  private long subscriberCount = 0;

  public static Interest create(String name, List<String> keywords) {
    Interest interest = new Interest();
    interest.name = name;
    keywords.stream()
        .map(k -> InterestKeyword.create(interest, k))
        .forEach(interest.keywords::add);
    return interest;
  }

  public void updateKeywords(List<String> newKeywords) {
    Set<String> toAdd = new HashSet<>(newKeywords);
    this.keywords.removeIf(k -> !toAdd.remove(k.getKeyword()));
    toAdd.stream()
        .map(k -> InterestKeyword.create(this, k))
        .forEach(this.keywords::add);
  }
}
