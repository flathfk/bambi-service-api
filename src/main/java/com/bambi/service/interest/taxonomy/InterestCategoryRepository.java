package com.bambi.service.interest.taxonomy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 관심사 카테고리 조회 저장소. */
public interface InterestCategoryRepository extends JpaRepository<InterestCategory, Long> {

    List<InterestCategory> findByTaxonomyVersionOrderByDisplayOrder(String taxonomyVersion);
}
