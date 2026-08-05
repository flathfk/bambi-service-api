package com.bambi.service.interest.taxonomy;

import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.interest.taxonomy.dto.InterestTaxonomyResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 프론트가 관심사 온보딩 목록을 조회하는 Service API. */
@RestController
@RequestMapping("/api/interest-taxonomy")
public class InterestTaxonomyController {

    private final InterestTaxonomyService taxonomyService;

    public InterestTaxonomyController(InterestTaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }

    /** 현재 활성 taxonomy와 카테고리·토픽 목록을 반환한다. */
    @GetMapping
    public ApiResponse<InterestTaxonomyResponse> getActive() {
        return ApiResponse.ok(taxonomyService.getActiveTaxonomy());
    }
}
