package com.sprint.mission.monew.domain.interest.util;

import com.sprint.mission.monew.domain.interest.config.SynonymProperties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SynonymUtils {

  private record SynonymEntry(String normalizedWord, int groupIndex) {}

  // 길이 내림차순 정렬 → longest match 보장
  private final List<SynonymEntry> suffixEntries;
  private final List<SynonymEntry> prefixEntries;
  private final List<String> rawSuffixWords;
  private final List<String> rawPrefixWords;

  public SynonymUtils(SynonymProperties props) {
    Objects.requireNonNull(props, "SynonymProperties must not be null");
    Objects.requireNonNull(props.suffixGroups(), "suffixGroups must not be null");
    Objects.requireNonNull(props.prefixGroups(), "prefixGroups must not be null");
    this.suffixEntries = buildEntries(props.suffixGroups());
    this.prefixEntries = buildEntries(props.prefixGroups());
    this.rawSuffixWords = extractRawWords(props.suffixGroups());
    this.rawPrefixWords = extractRawWords(props.prefixGroups());
  }

  public List<String> expandSearchTokens(List<String> rawTokens) {
    Set<String> result = new LinkedHashSet<>(rawTokens);
    for (String raw : rawTokens) {
      stripRawSuffix(raw).ifPresent(s -> {
        result.add(s);
        stripRawPrefix(s).ifPresent(result::add);
      });
      stripRawPrefix(raw).ifPresent(result::add);
    }
    return List.copyOf(result);
  }

  public double jaccardSimilarity(List<String> tokensA, List<String> tokensB) {
    Set<String> a = tokensA.stream().map(this::canonicalize).collect(Collectors.toSet());
    Set<String> b = tokensB.stream().map(this::canonicalize).collect(Collectors.toSet());
    long intersection = a.stream().filter(b::contains).count();
    long union = a.size() + b.size() - intersection;
    return union == 0 ? 1.0 : (double) intersection / union;
  }

  private Optional<String> stripRawSuffix(String raw) {
    for (String word : rawSuffixWords) {
      if (raw.endsWith(word) && raw.length() > word.length()) {
        return Optional.of(raw.substring(0, raw.length() - word.length()));
      }
    }
    return Optional.empty();
  }

  private Optional<String> stripRawPrefix(String raw) {
    for (String word : rawPrefixWords) {
      if (raw.startsWith(word) && raw.length() > word.length()) {
        return Optional.of(raw.substring(word.length()));
      }
    }
    return Optional.empty();
  }

  private String canonicalize(String token) {
    return matchSuffix(token)
        .map(e -> {
          String core = token.substring(0, token.length() - e.normalizedWord().length());
          return stripAnyPrefix(core) + "_S" + e.groupIndex();
        })
        .orElseGet(() -> matchPrefix(token)
            .map(e -> "P" + e.groupIndex() + "_" + token.substring(e.normalizedWord().length()))
            .orElse(token));
  }

  private String stripAnyPrefix(String core) {
    return matchPrefix(core)
        .map(e -> core.substring(e.normalizedWord().length()))
        .orElse(core);
  }

  private Optional<SynonymEntry> matchSuffix(String token) {
    for (SynonymEntry e : suffixEntries) {
      if (token.endsWith(e.normalizedWord())) {
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }

  private Optional<SynonymEntry> matchPrefix(String token) {
    for (SynonymEntry e : prefixEntries) {
      if (token.startsWith(e.normalizedWord()) && token.length() > e.normalizedWord().length()) {
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }

  private static List<SynonymEntry> buildEntries(List<Set<String>> groups) {
    List<SynonymEntry> entries = new ArrayList<>();
    for (int i = 0; i < groups.size(); i++) {
      for (String word : groups.get(i)) {
        entries.add(new SynonymEntry(JamoNormalizer.normalize(word), i));
      }
    }
    entries.sort(Comparator.comparingInt((SynonymEntry e) -> e.normalizedWord().length()).reversed());
    return List.copyOf(entries);
  }

  private static List<String> extractRawWords(List<Set<String>> groups) {
    return groups.stream()
        .flatMap(Collection::stream)
        .sorted(Comparator.comparingInt(String::length).reversed())
        .toList();
  }
}