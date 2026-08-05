package com.bambi.service.interest.taxonomy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 관심사 분류체계 버전 조회 저장소. */
public interface InterestTaxonomyVersionRepository extends JpaRepository<InterestTaxonomyVersion, String> {

    Optional<InterestTaxonomyVersion> findFirstByStatusOrderByPublishedAtDesc(String status);
}
