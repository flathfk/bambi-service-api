package com.bambi.service.interest.taxonomy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 관심사 토픽 조회 저장소. */
public interface InterestTopicRepository extends JpaRepository<InterestTopic, Long> {

    List<InterestTopic> findByTaxonomyVersion(String taxonomyVersion);

    Optional<InterestTopic> findByTaxonomyVersionAndTopicKey(String taxonomyVersion, String topicKey);
}
